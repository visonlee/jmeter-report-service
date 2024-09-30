package com.lws.jmeter.service;

import com.lws.jmeter.entity.Script;
import com.lws.jmeter.model.dto.ScriptDto;
import com.lws.jmeter.model.dto.ScriptJobDto;

import java.util.List;
import java.util.Optional;

public interface ScriptService {

    Optional<Script> findById(Long id);

    ScriptDto getScriptById(Long id);

    void save(Script script);

    List<ScriptDto> getAllScripts();

    void scheduleScript(ScriptJobDto scriptJobDto);

    void runPendingScripts();

    String generateJmeterReport(Long scriptId);

    void stopJmeterScript(Long scriptId);

    void deleteScript(Long scriptId);

}
