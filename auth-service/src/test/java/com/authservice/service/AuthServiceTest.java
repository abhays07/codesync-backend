package com.authservice.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.authservice.config.JwtUtils;
import com.authservice.entity.User;
import com.authservice.entity.Role;
import com.authservice.repository.UserRepository;
import com.authservice.serviceImpl.AuthServiceImpl;

import java.time.LocalDateTime;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1);
        testUser.setUsername("abhay_dev");
        testUser.setEmail("abhay@codesync.com");
        testUser.setPasswordHash("SecurePass123!");
        testUser.setRole(Role.DEVELOPER);
    }

    @Test
    void testSendRegistrationOtp_Success() {
        when(userRepository.existsByEmail("abhay@codesync.com")).thenReturn(false);
        when(userRepository.findByUsername("abhay_dev")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> authService.sendRegistrationOtp("abhay@codesync.com", "abhay_dev"));
    }

    @Test
    void testRegisterUser_Success() throws Exception {
        java.lang.reflect.Field field = AuthServiceImpl.class.getDeclaredField("registrationOtps");
        field.setAccessible(true);
        java.util.Map<String, String> map = (java.util.Map<String, String>) field.get(null);
        map.put("abhay@codesync.com", "123456");

        when(userRepository.existsByEmail("abhay@codesync.com")).thenReturn(false);
        when(userRepository.findByUsername("abhay_dev")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User savedUser = authService.register(testUser, "123456");
        assertNotNull(savedUser);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterUser_InvalidOtp_ShouldThrowException() throws Exception {
        java.lang.reflect.Field field = AuthServiceImpl.class.getDeclaredField("registrationOtps");
        field.setAccessible(true);
        java.util.Map<String, String> map = (java.util.Map<String, String>) field.get(null);
        map.clear();

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.register(testUser, "123456");
        });
        assertEquals("Invalid or expired registration OTP", exception.getMessage());
    }

    @Test
    void testLogin_Success() {
        when(userRepository.findByUsername("abhay_dev")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("SecurePass123!", "SecurePass123!")).thenReturn(true);
        when(jwtUtils.generateToken("abhay_dev", "DEVELOPER")).thenReturn("mockToken");

        String token = authService.login("abhay_dev", "SecurePass123!");
        assertEquals("mockToken", token);
    }

    @Test
    void testLogin_UserNotFound_ShouldThrowException() {
        when(userRepository.findByUsername("unknown_user")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login("unknown_user", "password");
        });
        assertEquals("Invalid username or password", exception.getMessage());
    }

    @Test
    void testGetUserById_Found() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        User found = authService.getUserById(1);
        assertEquals("abhay_dev", found.getUsername());
    }

    @Test
    void testGetUserById_NotFound() {
        when(userRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> authService.getUserById(1));
    }

    @Test
    void testUpdateProfile() {
        User details = new User();
        details.setFullName("Abhay Dev");
        details.setBio("Microservices Expert");
        details.setAvatarUrl("avatarUrl");

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User updated = authService.updateProfile(1, details);
        assertEquals("Abhay Dev", updated.getFullName());
        assertEquals("Microservices Expert", updated.getBio());
    }

    @Test
    void testSendPasswordResetOtp_Success() {
        when(userRepository.findByEmail("abhay@codesync.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(anyString())).thenReturn("hashedOtp");

        assertDoesNotThrow(() -> authService.sendPasswordResetOtp("abhay@codesync.com"));
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void testResetPasswordWithOtp_Success() {
        testUser.setResetOtp("hashedOtp");
        testUser.setOtpExpiry(LocalDateTime.now().plusMinutes(15));

        when(userRepository.findByEmail("abhay@codesync.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("123456", "hashedOtp")).thenReturn(true);
        when(passwordEncoder.encode("NewSecurePass123!")).thenReturn("newHashedPassword");

        assertDoesNotThrow(() -> authService.resetPasswordWithOtp("abhay@codesync.com", "123456", "NewSecurePass123!"));
        assertNull(testUser.getResetOtp());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void testRegisterUser_WeakPassword_ShouldThrowException() throws Exception {
        java.lang.reflect.Field field = AuthServiceImpl.class.getDeclaredField("registrationOtps");
        field.setAccessible(true);
        java.util.Map<String, String> map = (java.util.Map<String, String>) field.get(null);
        map.put("abhay@codesync.com", "123456");

        testUser.setPasswordHash("123");
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.register(testUser, "123456");
        });
        assertTrue(exception.getMessage().contains("Password must be at least 8 characters long"));
    }

    @Test
    void testResetPasswordWithOtp_ExpiredOtp_ShouldThrowException() {
        testUser.setResetOtp("hashedOtp");
        testUser.setOtpExpiry(LocalDateTime.now().minusMinutes(5));

        when(userRepository.findByEmail("abhay@codesync.com")).thenReturn(Optional.of(testUser));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.resetPasswordWithOtp("abhay@codesync.com", "123456", "NewSecurePass123!");
        });
        assertEquals("OTP expired or invalid", exception.getMessage());
    }

    @Test
    void testUpdateProfile_UsernameExists_ShouldThrowException() {
        User updatedUser = new User();
        updatedUser.setUsername("existing_user");
        updatedUser.setEmail("abhay@codesync.com");

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername("existing_user")).thenReturn(Optional.of(testUser));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.updateProfile(1, updatedUser);
        });
        assertEquals("Username already exists", exception.getMessage());
    }

    @Test
    void testUpdateProfile_EmailExists_ShouldThrowException() {
        User updatedUser = new User();
        updatedUser.setUsername("abhay_dev");
        updatedUser.setEmail("existing@codesync.com");

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("existing@codesync.com")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.updateProfile(1, updatedUser);
        });
        assertEquals("Email already exists", exception.getMessage());
    }

    @Test
    void testDeactivateAccount() {
        assertDoesNotThrow(() -> authService.deactivateAccount(1));
        verify(userRepository, times(1)).deleteById(1);
    }

    @Test
    void testRefreshToken_Success() {
        when(jwtUtils.getUsernameFromToken("mockToken")).thenReturn("abhay_dev");
        when(userRepository.findByUsername("abhay_dev")).thenReturn(Optional.of(testUser));
        when(jwtUtils.generateToken("abhay_dev", "DEVELOPER")).thenReturn("newMockToken");

        String newToken = authService.refreshToken("mockToken");
        assertEquals("newMockToken", newToken);
    }

    @Test
    void testChangePassword() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("NewSecurePass123!")).thenReturn("newHashedPassword");

        assertDoesNotThrow(() -> authService.changePassword(1, "NewSecurePass123!"));
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void testSendPasswordResetOtp_UserNotFound() {
        when(userRepository.findByEmail("nonexistent@codesync.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.sendPasswordResetOtp("nonexistent@codesync.com"));
    }

    @Test
    void testResetPasswordWithOtp_InvalidOtp() {
        testUser.setResetOtp("hashedOtp");
        testUser.setOtpExpiry(LocalDateTime.now().plusMinutes(15));

        when(userRepository.findByEmail("abhay@codesync.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongOtp", "hashedOtp")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.resetPasswordWithOtp("abhay@codesync.com", "wrongOtp", "NewSecurePass123!");
        });
        assertEquals("Invalid OTP", exception.getMessage());
    }

    @Test
    void testRegister_UsernameExists() throws Exception {
        java.lang.reflect.Field field = AuthServiceImpl.class.getDeclaredField("registrationOtps");
        field.setAccessible(true);
        java.util.Map<String, String> map = (java.util.Map<String, String>) field.get(null);
        map.put("abhay@codesync.com", "123456");

        when(userRepository.findByUsername("abhay_dev")).thenReturn(Optional.of(testUser));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.register(testUser, "123456");
        });
        assertEquals("Username already exists", exception.getMessage());
    }

    @Test
    void testRegister_EmailExists() throws Exception {
        java.lang.reflect.Field field = AuthServiceImpl.class.getDeclaredField("registrationOtps");
        field.setAccessible(true);
        java.util.Map<String, String> map = (java.util.Map<String, String>) field.get(null);
        map.put("abhay@codesync.com", "123456");

        when(userRepository.existsByEmail("abhay@codesync.com")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.register(testUser, "123456");
        });
        assertEquals("Email already exists", exception.getMessage());
    }

    @Test
    void testValidatePassword_MissingUpperCase() throws Exception {
        java.lang.reflect.Field field = AuthServiceImpl.class.getDeclaredField("registrationOtps");
        field.setAccessible(true);
        java.util.Map<String, String> map = (java.util.Map<String, String>) field.get(null);
        map.put("abhay@codesync.com", "123456");

        testUser.setPasswordHash("weakpass123!");
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.register(testUser, "123456");
        });
        assertEquals("Password must contain at least one uppercase letter", exception.getMessage());
    }

    @Test
    void testValidatePassword_MissingLowerCase() throws Exception {
        java.lang.reflect.Field field = AuthServiceImpl.class.getDeclaredField("registrationOtps");
        field.setAccessible(true);
        java.util.Map<String, String> map = (java.util.Map<String, String>) field.get(null);
        map.put("abhay@codesync.com", "123456");

        testUser.setPasswordHash("WEAKPASS123!");
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.register(testUser, "123456");
        });
        assertEquals("Password must contain at least one lowercase letter", exception.getMessage());
    }

    @Test
    void testValidatePassword_MissingDigit() throws Exception {
        java.lang.reflect.Field field = AuthServiceImpl.class.getDeclaredField("registrationOtps");
        field.setAccessible(true);
        java.util.Map<String, String> map = (java.util.Map<String, String>) field.get(null);
        map.put("abhay@codesync.com", "123456");

        testUser.setPasswordHash("WeakPass!");
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.register(testUser, "123456");
        });
        assertEquals("Password must contain at least one number", exception.getMessage());
    }

    @Test
    void testValidatePassword_MissingSpecialChar() throws Exception {
        java.lang.reflect.Field field = AuthServiceImpl.class.getDeclaredField("registrationOtps");
        field.setAccessible(true);
        java.util.Map<String, String> map = (java.util.Map<String, String>) field.get(null);
        map.put("abhay@codesync.com", "123456");

        testUser.setPasswordHash("WeakPass123");
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.register(testUser, "123456");
        });
        assertEquals("Password must contain at least one special character", exception.getMessage());
    }
}