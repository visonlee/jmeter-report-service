package com.lws.jmeter.model.dto;


import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Builder
@Data
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class ScriptJobDto {

    private Long scriptId;

    @NotNull(message = "expectedStartTime can not be null")
    @DateTimeFormat( pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expectedStartTime;
}
