package com.collabservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "collab_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CollabSession {
	@Id
	@Column(length = 36) // Standard length for UUID strings
	private String sessionId; // UUID session ID

	private int projectId;
	private int fileId; // Bound to a specific project file
	private int ownerId;

	@Column(length = 20)
	private String status; // ACTIVE or ENDED

	private String language;
	private boolean isPasswordProtected;
	private String sessionPassword;
	private int maxParticipants;

	private LocalDateTime createdAt = LocalDateTime.now();
	private LocalDateTime endedAt;
}