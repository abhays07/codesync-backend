package com.authservice.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.authservice.config.JwtUtils;
import com.authservice.entity.User;
import com.authservice.service.AuthService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Auth Controller - Handles Identity Traffic
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthResource {

	@Autowired
	private AuthService authService;

	@Autowired
	private JwtUtils jwtUtils;

	@PostMapping("/register")
	public ResponseEntity<User> registerUser(@Valid @RequestBody User user) {
		return ResponseEntity.ok(authService.register(user));
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody User loginRequest) {
		String token = authService.login(loginRequest.getUsername(), loginRequest.getPasswordHash());
		User user = authService.getByUsername(loginRequest.getUsername());

		Map<String, Object> response = new HashMap<>();
		response.put("token", token);
		response.put("userId", user.getUserId());
		response.put("username", user.getUsername());
		response.put("email", user.getEmail());
		response.put("role", user.getRole());
		response.put("avatarUrl", user.getAvatarUrl());

		return ResponseEntity.ok(response);
	}

	@GetMapping("/me")
	public ResponseEntity<?> getCurrentUser(Authentication authentication) {
		if (authentication == null)
			return ResponseEntity.status(401).body("Not authenticated");

		String email = (authentication
				.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User oAuth)
						? oAuth.getAttribute("email")
						: authentication.getName();

		User user = authService.getUserByEmail(email);
		String token = jwtUtils.generateToken(user.getUsername());

		Map<String, Object> response = new HashMap<>();
		response.put("token", token);
		response.put("userId", user.getUserId());
		response.put("username", user.getUsername());
		response.put("email", user.getEmail());
		response.put("avatarUrl", user.getAvatarUrl());
		return ResponseEntity.ok(response);
	}

	@GetMapping("/profile/{id}")
	public ResponseEntity<User> getProfile(@PathVariable int id) {
		return ResponseEntity.ok(authService.getUserById(id));
	}

	@PutMapping("/profile/{id}")
	public ResponseEntity<User> updateProfile(@PathVariable int id, @RequestBody User user) {
		return ResponseEntity.ok(authService.updateProfile(id, user));
	}

	@GetMapping("/search")
	public ResponseEntity<List<User>> searchUsers(@RequestParam String query) {
		return ResponseEntity.ok(authService.searchUsers(query));
	}

	@PostMapping("/send-otp")
	public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> request) {
		try {
			authService.sendPasswordResetOtp(request.get("email"));
			return ResponseEntity.ok(Map.of("message", "OTP sent successfully"));
		} catch (Exception e) {
			return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
		}
	}

	@PostMapping("/reset-password")
	public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
		try {
			authService.resetPasswordWithOtp(request.get("email"), request.get("otp"), request.get("newPassword"));
			return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
		} catch (Exception e) {
			return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
		}
	}
}