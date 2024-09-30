package com.lws.jmeter.controller;

import com.lws.jmeter.collector.CCTestResultCollector;
import com.lws.jmeter.config.StorageProperties;
import com.lws.jmeter.entity.Script;
import com.lws.jmeter.exception.StorageFileNotFoundException;
import com.lws.jmeter.model.dto.ScriptDto;
import com.lws.jmeter.model.dto.ScriptJobDto;
import com.lws.jmeter.model.dto.ScriptRunHistoryDto;
import com.lws.jmeter.service.ScriptRunHistoryService;
import com.lws.jmeter.service.ScriptService;
import com.lws.jmeter.service.StorageService;
import com.lws.jmeter.util.CompressUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.jmeter.JMeter;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.report.config.ConfigurationException;
import org.apache.jmeter.report.dashboard.GenerationException;
import org.apache.jmeter.report.dashboard.ReportGenerator;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.reporters.Summariser;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.visualizers.backend.BackendListener;
import org.apache.jorphan.collections.HashTree;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Controller
@Slf4j
@AllArgsConstructor
public class ScriptController {

	private final StorageService storageService;
	private final ScriptService scriptService;
	private final ScriptRunHistoryService scriptRunHistoryService;

	private final StorageProperties storageProperties;

	private final Executor executor = Executors.newFixedThreadPool(5);

	@GetMapping("/")
	public String home(Model model) {

		List<ScriptDto> allScripts = scriptService.getAllScripts();
		model.addAttribute("scripts",allScripts);

		return "dashboard";
	}

	@GetMapping("/script/download/{id}")
	@ResponseBody
	public ResponseEntity<Resource> saveFile(@PathVariable(value = "id") Long id) {


		Optional<Script> scriptOptional = scriptService.findById(id);
		if (scriptOptional.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		Script script = scriptOptional.get();

		Path path = Paths.get(script.getUploadedFullPath());

		Resource resource = null;
		try {
			resource = new UrlResource(path.toUri());
		} catch (MalformedURLException e) {
			throw new StorageFileNotFoundException("Could not read file: " + script.getFilename(), e);
		}
		if (!resource.exists() || !resource.isReadable()) {
			log.error("{} not found", resource);
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
				"attachment; filename=\"" + resource.getFilename() + "\"").body(resource);
	}

	@GetMapping("/report/download/{id}")
	@ResponseBody
	public ResponseEntity<Resource> reportDownload(@PathVariable(value = "id") Long id) {


		ScriptRunHistoryDto scriptRunHistoryDto = scriptRunHistoryService.getRunHistoryById(id);

		Path path = Paths.get(scriptRunHistoryDto.getReportPath());

		Resource resource = null;
		try {
			resource = new UrlResource(path.toUri());
		} catch (MalformedURLException e) {
			throw new StorageFileNotFoundException("Could not read file: " + scriptRunHistoryDto.getFilename(), e);
		}
		if (!resource.exists() || !resource.isReadable()) {
			log.error("{} not found", resource);
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
				"attachment; filename=\"" + resource.getFilename() + "\"").body(resource);
	}

	@GetMapping("/report/{filename}")
	@ResponseBody
	public ResponseEntity<Resource> report(@PathVariable String filename) throws Exception {

		Resource file = storageService.loadAsResource(filename);

		if (file == null)
			return ResponseEntity.notFound().build();

		String jtlFile = storageProperties.getLocation() + "/output.jtl"; //todo vince
		initJmeter(file.getFile(), jtlFile).run();
		String generateReport = generateReport(file.getFile(), jtlFile);

		File outZipFile = new File(generateReport);
		Path outPath = Paths.get(storageProperties.getLocation()).resolve(outZipFile.getName());
		Resource outFile = new UrlResource(outPath.toUri());

		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
				"attachment; filename=\"" + outFile.getFilename() + "\"").body(outFile);
	}

	@Deprecated
	@GetMapping("/start/{filename}")
	public String start_bak(@PathVariable String filename) throws Exception {

		Resource file = storageService.loadAsResource(filename);

		String jtlFile = storageProperties.getLocation() + "/output.jtl"; //todo vince
		StandardJMeterEngine standardJMeterEngine = initJmeter(file.getFile(), jtlFile);
		executor.execute(() -> {
            standardJMeterEngine.run();
            try {
                generateReport(file.getFile(), jtlFile);
            } catch (Exception e) {
                log.error("generate report error", e);
            }
        });
		return "redirect:/";
	}

	@PostMapping("/scheduleScript/{scriptId}")
	public String tryStart(@PathVariable(value = "scriptId") Long scriptId,
						   @ModelAttribute ScriptJobDto scriptJobDto) {

		scriptJobDto.setScriptId(scriptId);
		scriptService.scheduleScript(scriptJobDto);

		return "redirect:/";
	}

	@GetMapping("/scheduleScript/{scriptId}")
	public String scheduleScript(@PathVariable(value = "scriptId") Long scriptId, Model model) {
		ScriptDto scriptDto = scriptService.getScriptById(scriptId);
		model.addAttribute("script", scriptDto);
		model.addAttribute("scriptJobDto", new ScriptJobDto());

		return "scheduleScript";
	}

	@GetMapping("/scriptRunHistory/{scriptId}")
	public String scriptRunHistory(@PathVariable(value = "scriptId") Long scriptId, Model model) {
		ScriptDto scriptDto = scriptService.getScriptById(scriptId);
		List<ScriptRunHistoryDto> runHistories = scriptRunHistoryService.getRunHistoryByScriptId(scriptId);
		model.addAttribute("script", scriptDto);
		model.addAttribute("runHistories", runHistories);

		return "scriptRunHistory";
	}

	@GetMapping("/delete/{scriptId}")
	public String delete(@PathVariable(value = "scriptId") Long scriptId, Model model) {
		scriptService.deleteScript(scriptId);
		return "redirect:/";
	}

	@PostMapping("/")
	public String handleFileUpload(@RequestParam("file") MultipartFile file,
			RedirectAttributes redirectAttributes) {

		storageService.store(file);
		redirectAttributes.addFlashAttribute("message",
				"You successfully uploaded " + file.getOriginalFilename() + "!");

		return "redirect:/";
	}

	@ExceptionHandler(StorageFileNotFoundException.class)
	public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
		return ResponseEntity.notFound().build();
	}

	public StandardJMeterEngine initJmeter(File jmxFile, String outJtlFile) throws IOException {
		//Jmeter Engine
		StandardJMeterEngine jMeterEngine = new StandardJMeterEngine();

		String jmeterHome = "/Users/vince/tool/apache-jmeter-5.6.3";
		System.setProperty("jmeter.home", jmeterHome);
		File jmeter = new File(System.getProperty("jmeter.home"));

		if (!jmeter.exists()) {
			log.error("can not find JMeter home" );
			throw new RuntimeException("can not find JMeter home");
		}

		String slash = System.getProperty("file.separator");

		File jmeterProperties = new File(jmeter.getPath() + slash + "bin/jmeter.properties");
		if (!jmeterProperties.exists()) {
			log.error("can not find jmeter.properties" );
			throw new RuntimeException("can not find jmeter.properties");
		}

		JMeterUtils.setJMeterHome(jmeter.getPath());
		JMeterUtils.loadJMeterProperties(jmeterProperties.getPath());
		JMeterUtils.initLocale();

		SaveService.loadProperties();

		HashTree testPlanTree = SaveService.loadTree(jmxFile);

		Summariser summer = null;
		String summariserName = JMeterUtils.getPropDefault("summariser.name", "summary");

		if (!summariserName.isEmpty()) {
			summer = new Summariser(summariserName);
		}

		//generate jtl file
		ResultCollector resultCollector = new CCTestResultCollector(summer);
		resultCollector.setFilename(outJtlFile);
		testPlanTree.add(testPlanTree.getArray()[0], resultCollector);

		BackendListener backendListener = new BackendListener();

		backendListener.setEnabled(true);
		backendListener.setClassname("org.apache.jmeter.visualizers.backend.influxdb.InfluxdbBackendListenerClient");
		backendListener.setQueueSize("5000");
		backendListener.setName("Backend Listener");
		backendListener.setProperty("TestElement.gui_class", "org.apache.jmeter.visualizers.backend.BackendListenerGui");
		backendListener.setProperty("TestElement.test_class", "org.apache.jmeter.visualizers.backend.BackendListener");

		Arguments arguments = new Arguments();
		arguments.addArgument("influxdbMetricsSender", "org.apache.jmeter.visualizers.backend.influxdb.HttpMetricsSender");
		arguments.addArgument("influxdbUrl", "http://127.0.0.1:8086/write?db=jmeter");
		arguments.addArgument("application", "myJmeterTestDemo");
		arguments.addArgument("measurement", "jmeter");
		arguments.addArgument("summaryOnly", "true");
		arguments.addArgument("samplersRegex", ".*");
		arguments.addArgument("percentiles", "90;95;99");
		arguments.addArgument("testTitle","mytestname");
		arguments.addArgument("eventTags"," ");

		backendListener.setArguments(arguments);

		testPlanTree.add(testPlanTree.getArray()[0], backendListener);
		//runner Jmeter test
		jMeterEngine.configure(testPlanTree);

		return jMeterEngine;
	}

	@Deprecated
	public String generateReport(File jmxFile, String jtlFile) throws ConfigurationException, GenerationException, IOException, ArchiveException {
		//generate html report
		File outputDir = new File(storageProperties.getLocation() + "/report_" + jmxFile.getName() + new Date().getTime());
		if (!outputDir.exists()) {
			outputDir.mkdir();
		}

		JMeterUtils.setProperty(JMeter.JMETER_REPORT_OUTPUT_DIR_PROPERTY, outputDir.getAbsolutePath() );
		ReportGenerator reportGenerator = new ReportGenerator(jtlFile, null);
		reportGenerator.generate();

		String outZipFilePath = storageProperties.getLocation() + "/" + outputDir.getName() + ".zip";

		CompressUtils.compressDirectory(outputDir.getAbsolutePath(), outZipFilePath);

		return outZipFilePath;
	}

}
