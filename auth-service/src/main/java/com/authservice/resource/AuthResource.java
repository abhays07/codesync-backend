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

@RestController
@RequestMapping("/api/v1/auth") // Routed via API Gateway [cite: 16, 714]
public class AuthResource {

	@Autowired
	private AuthService authService;

	@Autowired
	private JwtUtils jwtUtils;

	@PostMapping("/register")
	public ResponseEntity<User> registerUser(@Valid @RequestBody User user) {
		// @Valid triggers JSR-303 constraints defined in the User entity
		return ResponseEntity.ok(authService.register(user));
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody User loginRequest) {
		// 1. Authenticate user and generate token
		String token = authService.login(loginRequest.getUsername(), loginRequest.getPasswordHash());

		// 2. Fetch the user details to get the actual ID
		User user = authService.getByUsername(loginRequest.getUsername());

		// 3. Return a Map or a DTO instead of just a string
		Map<String, Object> response = new HashMap<>();
		response.put("token", token);
		response.put("userId", user.getUserId());
		response.put("username", user.getUsername());
		response.put("email", user.getEmail());

		return ResponseEntity.ok(response);
	}

	@GetMapping("/me")
	public ResponseEntity<?> getCurrentUser(Authentication authentication) {
		String email;

		// Check if it's an OAuth2 user or a standard JWT user
		if (authentication
				.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User oAuth2User) {
			email = oAuth2User.getAttribute("email");
		} else {
			email = authentication.getName();
		}

		// Use getUserByEmail or getByUsername based on what you stored during
		// CustomOAuth2UserService
		User user = authService.getUserByEmail(email);

		String token = jwtUtils.generateToken(user.getUsername());

		Map<String, Object> response = new HashMap<>();
		response.put("token", token);
		response.put("userId", user.getUserId());
		response.put("username", user.getUsername());
		return ResponseEntity.ok(response);
	}

	@GetMapping("/profile/{id}")
	public ResponseEntity<User> getProfile(@PathVariable int id) {
		// Retrieval by ID as specified in Auth/User-Service design
		return ResponseEntity.ok(authService.getUserById(id));
	}

	@PutMapping("/profile/{id}")
	public ResponseEntity<User> updateProfile(@PathVariable int id, @RequestBody User user) {
		// Allows developers to update their own bio and profile data
		return ResponseEntity.ok(authService.updateProfile(id, user));
	}
	
	/**
     * Update Password
     * Requirement: Password must be encoded before saving (handled in ServiceImpl)
     */
    @PatchMapping("/password/{userId}")
    public ResponseEntity<String> changePassword(
            @PathVariable int userId, 
            @RequestBody Map<String, String> passwordRequest) {
        
        String newPassword = passwordRequest.get("newPassword");
        if (newPassword == null || newPassword.length() < 8) {
            return ResponseEntity.badRequest().body("Password must be at least 8 characters.");
        }

        authService.changePassword(userId, newPassword);
        return ResponseEntity.ok("Password updated successfully.");
    }

    /**
     * Deactivate Account (Soft Delete)
     * Requirement: Set isActive = false as per Case Study
     */
    @PutMapping("/deactivate/{userId}")
    public ResponseEntity<String> deactivateAccount(@PathVariable int userId) {
        authService.deactivateAccount(userId);
        return ResponseEntity.ok("Account deactivated successfully. You will be logged out.");
    }

    /**
     * Search Users
     * Requirement: Real-time search for collaboration/mentorship
     */
    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String query) {
        return ResponseEntity.ok(authService.searchUsers(query));
    }
}