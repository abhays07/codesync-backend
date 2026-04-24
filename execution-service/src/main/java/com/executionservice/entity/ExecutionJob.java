package com.executionservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "execution_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionJob {
	@Id
	private String jobId; // UUID
	private int projectId;
	private int fileId;
	private int userId;
	private String language;

	@Column(columnDefinition = "TEXT")
	private String sourceCode;

	@Column(columnDefinition = "TEXT")
	private String stdin;

	private String status; // QUEUED, RUNNING, COMPLETED, FAILED [cite: 52]

	@Column(columnDefinition = "TEXT")
	private String stdout;

	@Column(columnDefinition = "TEXT")
	private String stderr;

	private int exitCode;
	private long executionTimeMs;
	private long memoryUsedKb;
	private LocalDateTime createdAt = LocalDateTime.now();
	private LocalDateTime completedAt;
}