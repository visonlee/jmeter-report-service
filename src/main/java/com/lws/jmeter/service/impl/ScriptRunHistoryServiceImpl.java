package com.lws.jmeter.service.impl;

import com.lws.jmeter.entity.ScriptRunHistory;
import com.lws.jmeter.model.dto.ScriptRunHistoryDto;
import com.lws.jmeter.repository.ScriptRunHistoryRepository;
import com.lws.jmeter.service.ScriptRunHistoryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class ScriptRunHistoryServiceImpl implements ScriptRunHistoryService {
    private ScriptRunHistoryRepository scriptRunHistoryRepository;

    @Override
    public ScriptRunHistoryDto getRunHistoryById(Long id) {
        ScriptRunHistory scriptRunHistory = scriptRunHistoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("can not find script run history by id: " + id));
        return ScriptRunHistoryDto.convert(scriptRunHistory);
    }

    @Override
    public List<ScriptRunHistoryDto> getRunHistoryByScriptId(Long scriptId) {
        List<ScriptRunHistory> histories = scriptRunHistoryRepository.getRunHistoryByScriptId(scriptId);
        return histories.stream().map(ScriptRunHistoryDto::convert).toList();
    }
}
