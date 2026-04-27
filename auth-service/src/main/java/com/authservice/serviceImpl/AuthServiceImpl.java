package com.authservice.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.authservice.config.JwtUtils;
import com.authservice.entity.User;
import com.authservice.repository.UserRepository;
import com.authservice.service.AuthService;
import com.authservice.service.EmailService;
import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private JwtUtils jwtUtils;
	@Autowired
	private EmailService emailService;

	@Override
	public User register(User user) {
		// Validation: Ensure email/username uniqueness before hashing
		if (userRepository.existsByEmail(user.getEmail()))
			throw new RuntimeException("Email already registered");

		user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
		User savedUser = userRepository.save(user);

		// Non-blocking: Sends email in a background thread to keep response time fast
		new Thread(() -> emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getUsername())).start();
		return savedUser;
	}

	@Override
	public String login(String username, String password) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("Invalid username or password"));

		if (passwordEncoder.matches(password, user.getPasswordHash())) {
			return jwtUtils.generateToken(username);
		} else {
			throw new RuntimeException("Invalid username or password");
		}
	}

	@Override
	public User getUserById(int userId) {
		return userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User profile not found"));
	}

	@Override
	public List<User> searchUsers(String query) {
		return userRepository.searchByUsername(query);
	}

	@Override
	public boolean validateToken(String token) {
		return jwtUtils.validateToken(token);
	}

	@Override
	public User getUserByEmail(String email) {
		return userRepository.findByEmail(email).orElse(null);
	}

	@Override
	public User getByUsername(String username) {
		return userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
	}

	@Override
	public User updateProfile(int userId, User userDetails) {
		User user = getUserById(userId);
		user.setFullName(userDetails.getFullName());
		user.setBio(userDetails.getBio());
		user.setAvatarUrl(userDetails.getAvatarUrl());
		if (userDetails.getUsername() != null && !userDetails.getUsername().trim().isEmpty()) {
			user.setUsername(userDetails.getUsername().trim());
		}
		user.setGithubLink(userDetails.getGithubLink());
		user.setLinkedinLink(userDetails.getLinkedinLink());
		user.setTwitterLink(userDetails.getTwitterLink());
		return userRepository.save(user);
	}

	@Override
	public void changePassword(int userId, String newPassword) {
		User user = getUserById(userId);
		user.setPasswordHash(passwordEncoder.encode(newPassword));
		userRepository.save(user);
	}

	@Override
	public void sendPasswordResetOtp(String email) {
		User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found with this email"));
		
		// Generate 6-digit OTP
		String otp = String.format("%06d", new java.util.Random().nextInt(999999));
		
		user.setResetOtp(otp);
		user.setOtpExpiry(java.time.LocalDateTime.now().plusMinutes(10));
		userRepository.save(user);
		
		new Thread(() -> emailService.sendOtpEmail(email, otp)).start();
	}

	@Override
	public void resetPasswordWithOtp(String email, String otp, String newPassword) {
		User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
		
		if (user.getResetOtp() == null || !user.getResetOtp().equals(otp)) {
			throw new RuntimeException("Invalid OTP");
		}
		
		if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(java.time.LocalDateTime.now())) {
			throw new RuntimeException("OTP has expired");
		}
		
		// Reset password and clear OTP
		user.setPasswordHash(passwordEncoder.encode(newPassword));
		user.setResetOtp(null);
		user.setOtpExpiry(null);
		
		userRepository.save(user);
	}

	@Override
	public void deactivateAccount(int userId) {
		User user = getUserById(userId);
		user.setActive(false); // Requirement: Soft Delete only
		userRepository.save(user);
	}

	// Unused in JWT stateless context, but kept for interface compliance
	@Override
	public void logout(String token) {
	}

	@Override
	public String refreshToken(String token) {
		return jwtUtils.generateToken(jwtUtils.getUsernameFromToken(token));
	}
}