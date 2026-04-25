package com.projectservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMember {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private int projectId;
	private int userId;
	
	private String username;

	@Column(length = 20)
	private String role; // e.g., "EDITOR", "PENDING"

	private LocalDateTime joinedAt = LocalDateTime.now();
}