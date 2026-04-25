package com.notificationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Integer recipientId;
	private Integer senderId; // The ID of the person making the request
	private Long relatedId; // The Project ID or File ID
	private String senderName;

	private String senderEmail;

	private String message;
	private String type;
	private boolean isRead = false;
	private LocalDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
	}
}
