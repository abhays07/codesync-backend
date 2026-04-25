package com.commentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Integer fileId;
	private Integer userId;
	private String username;

	@Column(columnDefinition = "TEXT")
	private String content;

	// For Code Review functionality (Requirement R2.10)
	private Integer lineNumber;

	// For Threaded Replies
	private Long parentCommentId;

	private LocalDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
	}
}