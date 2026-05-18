package com.authservice.resourceTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.authservice.entity.User;
import com.authservice.entity.Role;
import com.authservice.resource.AuthResource;
import com.authservice.service.AuthService;
import com.authservice.config.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class AuthResourceTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthResource authResource;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authResource).build();

        testUser = new User();
        testUser.setUserId(1);
        testUser.setUsername("abhay_dev");
        testUser.setEmail("abhay@codesync.com");
        testUser.setPasswordHash("SecurePass123!");
        testUser.setRole(Role.DEVELOPER);
    }

    @Test
    void testRegisterUser() throws Exception {
        when(authService.register(any(User.class), eq("123456"))).thenReturn(testUser);

        mockMvc.perform(post("/api/v1/auth/register?otp=123456")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("abhay_dev"));
    }

    @Test
    void testSendRegistrationOtp_Success() throws Exception {
        doNothing().when(authService).sendRegistrationOtp("abhay@codesync.com", "abhay_dev");

        Map<String, String> request = new HashMap<>();
        request.put("email", "abhay@codesync.com");
        request.put("username", "abhay_dev");

        mockMvc.perform(post("/api/v1/auth/send-registration-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registration OTP sent successfully"));
    }

    @Test
    void testLogin_Success() throws Exception {
        when(authService.login("abhay_dev", "SecurePass123!")).thenReturn("mockToken");
        when(authService.getByUsername("abhay_dev")).thenReturn(testUser);

        User loginRequest = new User();
        loginRequest.setUsername("abhay_dev");
        loginRequest.setPasswordHash("SecurePass123!");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mockToken"))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void testGetProfile() throws Exception {
        when(authService.getUserById(1)).thenReturn(testUser);

        mockMvc.perform(get("/api/v1/auth/profile/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("abhay_dev"));
    }

    @Test
    void testUpdateProfile() throws Exception {
        when(authService.updateProfile(eq(1), any(User.class))).thenReturn(testUser);

        mockMvc.perform(put("/api/v1/auth/profile/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("abhay_dev"));
    }

    @Test
    void testGetCurrentUser_Success() throws Exception {
        when(jwtUtils.validateToken("mockToken")).thenReturn(true);
        when(jwtUtils.getUsernameFromToken("mockToken")).thenReturn("abhay_dev");
        when(authService.getByUsername("abhay_dev")).thenReturn(testUser);

        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer mockToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("abhay_dev"))
                .andExpect(jsonPath("$.email").value("abhay@codesync.com"));
    }

    @Test
    void testGetCurrentUser_NotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetCurrentUser_OAuth2Success() throws Exception {
        org.springframework.security.core.Authentication authentication = org.mockito.Mockito.mock(org.springframework.security.core.Authentication.class);
        org.springframework.security.oauth2.core.user.OAuth2User oAuth2User = org.mockito.Mockito.mock(org.springframework.security.oauth2.core.user.OAuth2User.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("oauth_user");
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email")).thenReturn("abhay@codesync.com");

        when(authService.getUserByEmail("abhay@codesync.com")).thenReturn(testUser);
        when(jwtUtils.generateToken("abhay_dev", "DEVELOPER")).thenReturn("oauthMockToken");

        mockMvc.perform(get("/api/v1/auth/me")
                .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("oauthMockToken"));
    }

    @Test
    void testGetCurrentUser_StandardAuthenticationSuccess() throws Exception {
        org.springframework.security.core.Authentication authentication = org.mockito.Mockito.mock(org.springframework.security.core.Authentication.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("abhay@codesync.com");
        when(authentication.getPrincipal()).thenReturn("not_oauth2_user");

        when(authService.getUserByEmail("abhay@codesync.com")).thenReturn(testUser);
        when(jwtUtils.generateToken("abhay_dev", "DEVELOPER")).thenReturn("stdMockToken");

        mockMvc.perform(get("/api/v1/auth/me")
                .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("stdMockToken"));
    }

    @Test
    void testSearchUsers() throws Exception {
        java.util.List<User> list = java.util.List.of(testUser);
        when(authService.searchUsers("abhay")).thenReturn(list);

        mockMvc.perform(get("/api/v1/auth/search?query=abhay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("abhay_dev"));
    }

    @Test
    void testSendOtp_Success() throws Exception {
        doNothing().when(authService).sendPasswordResetOtp("abhay@codesync.com");

        Map<String, String> request = new HashMap<>();
        request.put("email", "abhay@codesync.com");

        mockMvc.perform(post("/api/v1/auth/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP sent successfully"));
    }

    @Test
    void testResetPassword_Success() throws Exception {
        doNothing().when(authService).resetPasswordWithOtp("abhay@codesync.com", "123456", "NewSecurePass123!");

        Map<String, String> request = new HashMap<>();
        request.put("email", "abhay@codesync.com");
        request.put("otp", "123456");
        request.put("newPassword", "NewSecurePass123!");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully"));
    }
}
