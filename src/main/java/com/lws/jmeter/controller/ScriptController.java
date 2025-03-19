package com.lws.jmeter.controller;

import com.lws.jmeter.config.StorageProperties;
import com.lws.jmeter.entity.Script;
import com.lws.jmeter.exception.StorageFileNotFoundException;
import com.lws.jmeter.model.dto.ScriptDto;
import com.lws.jmeter.model.dto.ScriptJobDto;
import com.lws.jmeter.model.dto.ScriptRunHistoryDto;
import com.lws.jmeter.service.ScriptRunHistoryService;
import com.lws.jmeter.service.ScriptService;
import com.lws.jmeter.service.StorageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
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

}
