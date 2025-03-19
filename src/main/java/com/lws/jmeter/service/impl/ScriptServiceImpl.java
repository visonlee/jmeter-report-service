package com.lws.jmeter.service.impl;

import com.lws.jmeter.collector.CCTestResultCollector;
import com.lws.jmeter.entity.Script;
import com.lws.jmeter.entity.ScriptJob;
import com.lws.jmeter.entity.ScriptRunHistory;
import com.lws.jmeter.model.ScriptStatus;
import com.lws.jmeter.model.dto.ScriptDto;
import com.lws.jmeter.model.dto.ScriptJobDto;
import com.lws.jmeter.repository.ScriptJobRepository;
import com.lws.jmeter.repository.ScriptRepository;
import com.lws.jmeter.repository.ScriptRunHistoryRepository;
import com.lws.jmeter.service.ScriptService;
import com.lws.jmeter.util.CompressUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.jmeter.JMeter;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.report.dashboard.ReportGenerator;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.reporters.Summariser;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.visualizers.backend.BackendListener;
import org.apache.jorphan.collections.HashTree;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Service
@AllArgsConstructor
@Slf4j
public class ScriptServiceImpl implements ScriptService {

    private ScriptRepository scriptRepository;
    private ScriptJobRepository scriptJobRepository;
    private ScriptRunHistoryRepository scriptRunHistoryRepository;

    private final Executor executor = Executors.newFixedThreadPool(5);

    Map<Long, StandardJMeterEngine> jMeterEngineMap = new ConcurrentHashMap<>();

    public Optional<Script> findById(Long id) {
        return scriptRepository.findById(id);
    }

    @Override
    public ScriptDto getScriptById(Long id) {
        Script script = scriptRepository.findById(id).orElseThrow(() -> new RuntimeException("can not find script by id: " + id));
        return ScriptDto.convert(script);
    }

    public void save(Script script) {
        scriptRepository.save(script);
    }

    public List<ScriptDto> getAllScripts() {
        List<Script> scripts = scriptRepository.findAll();
        return scripts.stream().map(ScriptDto::convert).toList();
    }


    @Transactional // todo vince
    public void scheduleScript(ScriptJobDto scriptJobDto) {
        Script script = this.findById(scriptJobDto.getScriptId())
                .orElseThrow(() -> new RuntimeException("script not found"));// todo vince
        if (script.getStatus() == ScriptStatus.RUNNING || script.getStatus() == ScriptStatus.WAITING) {
            throw new RuntimeException("illegal script status");
        }

        if (scriptJobDto.getExpectedStartTime().isBefore(LocalDateTime.now())) {
            //todo vince, need to aad below logic
//            throw new RuntimeException("illegal script expectedStartTime");
        }

        ScriptJob scriptJob = ScriptJob.builder()
                .scriptId(scriptJobDto.getScriptId())
                .expectedStartTime(scriptJobDto.getExpectedStartTime())
                .build();
        scriptJobRepository.save(scriptJob);

        script.setStatus(ScriptStatus.WAITING);
        scriptRepository.save(script);
    }

    @Override
    public void runPendingScripts() {
        List<ScriptJob> scriptJobs = scriptJobRepository.fetchPendingScriptJobs();
        log.info("{} jobs need to handle", scriptJobs.size());

        for (ScriptJob job : scriptJobs) {
            try {
                runScript(job.getScriptId());
            }catch (Exception e) { // todo if any error, delete the record, if any better solution?
                log.error("fail to run script {}", job.getScriptId(), e);
            }
        }
    }

    @Override
    public String generateJmeterReport(Long scriptId)  {
        Script script = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("can not find script with script id:" + scriptId));
//        if (script.getStatus() != ScriptStatus.COMPLETED || script.getStatus() != ScriptStatus.CANCELLED) {
//            log.error("script not completed" );
//            throw new RuntimeException("script not completed");
//        }
        //generate html report
        File outputDir = new File(script.getReportDirectory() + "/report_" + new Date().getTime());
        if (!outputDir.exists()) {
            outputDir.mkdir();
        }

        JMeterUtils.setProperty(JMeter.JMETER_REPORT_OUTPUT_DIR_PROPERTY, outputDir.getAbsolutePath() );
        String jtlFile = script.getReportDirectory() + "/" + FilenameUtils.getBaseName(script.getFilename()) + ".jtl";
        ReportGenerator reportGenerator = null;
        try {
            reportGenerator = new ReportGenerator(jtlFile, null);
            reportGenerator.generate();

        } catch (Exception e) {
            log.error("fail to generate report from jtl", e);
            throw new RuntimeException("fail to generate report from jtl", e);
        }

        String outZipFilePath = script.getReportDirectory() + "/" + outputDir.getName() + ".zip";

        try {
            CompressUtils.compressDirectory(outputDir.getAbsolutePath(), outZipFilePath);
        } catch (Exception e) {
            log.error("fail to compress report", e);
            throw new RuntimeException("fail to compress report", e);
        }

        return outZipFilePath;
    }

    @Override
    public void stopJmeterScript(Long scriptId) {
        Script script = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("can not find script with script id:" + scriptId));
        StandardJMeterEngine jMeterEngine = jMeterEngineMap.remove(scriptId);
        jMeterEngine.stopTest();
        script.setStatus(ScriptStatus.CANCELLED);
        script.setEndTime(LocalDateTime.now());
        scriptRepository.save(script);
    }

    @Override
    @Transactional
    public void deleteScript(Long scriptId) { // TODO vince put then into a transaction ?
        // delete folder
        Script script = scriptRepository.findById(scriptId).orElseThrow(() -> new RuntimeException("can not find scriptId: " + scriptId));

        Path directoryToDelete = Paths.get(script.getUploadedFullPath()).getParent();

        if (directoryToDelete.toFile().exists()) {
            boolean success = FileSystemUtils.deleteRecursively(directoryToDelete.toFile());
            if (!success) {
                log.error("fail to delete {}", directoryToDelete);
                throw new RuntimeException("fail to delete " + directoryToDelete);
            }
        }

        scriptRepository.deleteById(scriptId);
        scriptJobRepository.deleteByScriptId(scriptId);
        scriptRunHistoryRepository.deleteByScriptId(scriptId);

        //todo vince any more need to be delete?
    }


    private void runScript(Long scriptId) throws IOException {

        Script script = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("can not find script with script id:" + scriptId));

        ScriptJob scriptJob = scriptJobRepository.findByScriptId(scriptId);

        if (script.getStatus() != ScriptStatus.WAITING ) {// todo refactor
            log.warn("illegal status with script id {}", script.getId());
            throw new RuntimeException("illegal status with script id: " + script.getId());
        }

        String reportDirectory = script.getReportDirectory();
        String jtlFIlePath = reportDirectory + "/" + FilenameUtils.getBaseName(script.getFilename()) + ".jtl"; // todo vince need refactor
        StandardJMeterEngine standardJMeterEngine = initPerformanceTest(new File(script.getExtractedFullPath()), jtlFIlePath);

        jMeterEngineMap.put(scriptId, standardJMeterEngine);

        //todo run the script
        script.setStatus(ScriptStatus.RUNNING);
        script.setStartTime(LocalDateTime.now());
        scriptRepository.save(script);

        ScriptRunHistory scriptRunHistory = ScriptRunHistory.builder().scriptId(scriptId).startTime(LocalDateTime.now()).build();
        scriptRunHistoryRepository.save(scriptRunHistory);


        executor.execute(() -> {
            try {
                standardJMeterEngine.run();
                jMeterEngineMap.remove(scriptId);

                script.setEndTime(LocalDateTime.now());

                String compressReportFilePath = generateJmeterReport(scriptId);
                scriptRunHistory.setReportPath(compressReportFilePath);
                scriptRunHistory.setEndTime(LocalDateTime.now());
                scriptRunHistoryRepository.save(scriptRunHistory);

                script.setStatus(ScriptStatus.COMPLETED);
                scriptRepository.save(script);

                scriptJobRepository.delete(scriptJob);

            }catch (Exception e) {
                log.error("run JMeter script error", e);
                script.setStatus(ScriptStatus.ERROR);
                scriptRepository.save(script);
                jMeterEngineMap.remove(scriptId);
            }
        });
    }


    private StandardJMeterEngine initPerformanceTest(File jmxFile, String outJtlFile) throws IOException {
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

        setBackendListener(testPlanTree); // todo need to add backÏ

        //runner Jmeter test
        jMeterEngine.configure(testPlanTree);

        return jMeterEngine;
    }

    private void setBackendListener(HashTree testPlanTree) {
//        if (!"true".equalsIgnoreCase(grafanaEnabled)) return;

        BackendListener backendListener = new BackendListener(); // todo extract methods
        backendListener.setEnabled(true);
        backendListener.setClassname("org.apache.jmeter.visualizers.backend.influxdb.InfluxdbBackendListenerClient");
        backendListener.setQueueSize("5000");
        backendListener.setName("Backend Listener");
        backendListener.setProperty("TestElement.gui_class", "org.apache.jmeter.visualizers.backend.BackendListenerGui");
        backendListener.setProperty("TestElement.test_class", "org.apache.jmeter.visualizers.backend.BackendListener");

        // dataSource influxDb
        Arguments arguments = new Arguments();
        arguments.addArgument("influxdbMetricsSender", "org.apache.jmeter.visualizers.backend.influxdb.HttpMetricsSender");
        arguments.addArgument("influxdbUrl", "http://127.0.0.1:8086/write?db=jmeter");
        arguments.addArgument("application", "myJmeterTestDemo"); //todo vince
        arguments.addArgument("measurement", "jmeter");
        arguments.addArgument("summaryOnly", "true");
        arguments.addArgument("samplersRegex", ".*");
        arguments.addArgument("percentiles", "90;95;99");
        arguments.addArgument("testTitle","mytestname");
        arguments.addArgument("eventTags"," ");

        backendListener.setArguments(arguments);

        testPlanTree.add(testPlanTree.getArray()[0], backendListener);
    }

}
