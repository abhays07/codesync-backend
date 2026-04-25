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
}