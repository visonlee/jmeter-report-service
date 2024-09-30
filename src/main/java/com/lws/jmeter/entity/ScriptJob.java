package com.lws.jmeter.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "script_job")
@Builder
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ScriptJob implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long scriptId;
    private LocalDateTime expectedStartTime;
    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createTime;
}