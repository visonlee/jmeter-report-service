package com.lws.jmeter.service;

import com.lws.jmeter.model.dto.ScriptRunHistoryDto;

import java.util.List;

public interface ScriptRunHistoryService {


    ScriptRunHistoryDto getRunHistoryById(Long id);

    List<ScriptRunHistoryDto> getRunHistoryByScriptId(Long scriptId);

}
