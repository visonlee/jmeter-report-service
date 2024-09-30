package com.lws.jmeter.entity;

import com.lws.jmeter.model.ScriptStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "script")
@Builder
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Script implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String filename;
    private String uploadedFullPath;
    private String extractedFullPath;
    private String reportDirectory;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    @Enumerated
    @Column(nullable = false)
    private ScriptStatus status;
    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createTime;
    @UpdateTimestamp
    private LocalDateTime updateTime;
}