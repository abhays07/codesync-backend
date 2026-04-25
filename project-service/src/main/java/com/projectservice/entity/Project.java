package com.projectservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Project {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int projectId;

	private int ownerId; // Foreign reference to User service
	private String name;
	private String description;
	private String language; // e.g., Java, Python

	private String visibility; // PUBLIC or PRIVATE
	private int templateId; // For starter code

	private boolean isArchived = false; //
	private LocalDateTime createdAt = LocalDateTime.now();
	private LocalDateTime updatedAt = LocalDateTime.now();

	@Transient
	@JsonProperty("isStarredByMe")
	private boolean isStarredByMe; // Not stored in DB, calculated at runtime

	// Added manually because Jackson to include this in the JSON
	@JsonProperty("isStarredByMe")
	public boolean isStarredByMe() {
		return isStarredByMe;
	}

	public void setStarredByMe(boolean starredByMe) {
		this.isStarredByMe = starredByMe;
	}

	private int starCount = 0;
	private int forkCount = 0;
}