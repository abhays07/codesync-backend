package com.authservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int userId;

	@NotBlank(message = "Username is required")
	@Size(min = 4, max = 20, message = "Username must be between 4-20 characters")
	@Column(unique = true, nullable = false)
	private String username;

	@NotBlank(message = "Email is required")
	@Email(message = "Please provide a valid email")
	@Column(unique = true, nullable = false)
	private String email;

	@NotBlank(message = "Password cannot be empty")
	@Size(min = 8, message = "Password must be at least 8 characters")
	private String passwordHash;

	private String fullName;

	@Column(name = "user_role")
	private String role; // DEVELOPER, ADMIN

	private String avatarUrl;
	private String provider; // LOCAL, GITHUB, GOOGLE

	private boolean isActive = true;
	private LocalDateTime createdAt = LocalDateTime.now();
	private String bio;

	// Required constructor
	public User(int userId) {
		this.userId = userId;
	}
}