package com.lws.jmeter.model.dto;

import com.lws.jmeter.entity.Script;
import com.lws.jmeter.model.ScriptStatus;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScriptDto {
    private Long id;
    private String username;
    private String filename;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;

    public static ScriptDto convert(Script entity) {
        return ScriptDto.builder()
                .id(entity.getId())
                .username("深仔") // todo vince
                .filename(entity.getFilename())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .status(entity.getStatus().toString())
                .build();
    }

    public boolean isRunAllowed() {
        return !ScriptStatus.WAITING.toString().equalsIgnoreCase(this.status) &&
                !ScriptStatus.RUNNING.toString().equalsIgnoreCase(this.status);
    }

    public boolean isStopAllowed() {
        return ScriptStatus.RUNNING.toString().equalsIgnoreCase(this.status) ;
    }
}
