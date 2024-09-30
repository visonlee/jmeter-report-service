package com.lws.jmeter.model.dto;

import com.lws.jmeter.entity.ScriptRunHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.nio.file.Paths;
import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScriptRunHistoryDto {


    private Long id;
    private String filename;
    private String reportPath;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public static ScriptRunHistoryDto convert(ScriptRunHistory entity) {
        String filename = "";
        if (StringUtils.hasLength(entity.getReportPath())) {
            filename = Paths.get(entity.getReportPath()).getFileName().toString();
        }

        return ScriptRunHistoryDto.builder()
                .id(entity.getId())
                .reportPath(entity.getReportPath())
                .filename(filename)
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .build();
    }
}
