package com.authservice.service;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.authservice.entity.User;
import com.authservice.repository.UserRepository;
import com.authservice.serviceImpl.AuthServiceImpl;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@InjectMocks
	private AuthServiceImpl authService;

	@Test
	void testRegisterUser_Success() {
		// Arrange
		User user = new User();
		user.setUsername("abhay_dev");
		user.setPasswordHash("rawPassword");

		when(passwordEncoder.encode("rawPassword")).thenReturn("hashedPassword");
		when(userRepository.save(any(User.class))).thenReturn(user);

		// Act
		User savedUser = authService.register(user);

		// Assert
		assertNotNull(savedUser);
		assertEquals("hashedPassword", user.getPasswordHash());
		verify(userRepository, times(1)).save(user);
	}
}