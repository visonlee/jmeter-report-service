package com.lws.jmeter.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "script_run_history")
@Builder
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ScriptRunHistory implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long scriptId;
    private String reportPath;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    @CreationTimestamp
    private LocalDateTime createTime;
    @UpdateTimestamp
    private LocalDateTime updateTime;
}