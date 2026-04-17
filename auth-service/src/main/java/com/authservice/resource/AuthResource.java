package com.authservice.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import com.authservice.entity.User;
import com.authservice.service.AuthService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth") // Routed via API Gateway [cite: 16, 714]
public class AuthResource {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<User> registerUser(@Valid @RequestBody User user) {
        // @Valid triggers JSR-303 constraints defined in the User entity
        return ResponseEntity.ok(authService.register(user)); 
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Map<String, String> credentials) {
        // Requirement: Authentication via username and password
        String token = authService.login(credentials.get("username"), credentials.get("password"));
        return ResponseEntity.ok(token); 
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
}