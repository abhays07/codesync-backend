package com.authservice.config;

import com.authservice.serviceImpl.CustomOAuth2UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;

/**
 * Security Configuration - Centralizes Auth Rules Standardizes Password
 * Encoding and OAuth2 Integration
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final CustomOAuth2UserService customOAuth2UserService;
	private final String frontendUrl;
	private final JwtUtils jwtService;

	public SecurityConfig(CustomOAuth2UserService customOAuth2UserService, @Value("${FRONTEND_URL}") String frontendUrl,
			JwtUtils jwtService) {
		this.customOAuth2UserService = customOAuth2UserService;
		this.frontendUrl = frontendUrl;
		this.jwtService = jwtService;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		// Industry Standard: BCrypt for one-way password hashing
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(
						auth -> auth.requestMatchers("/api/v1/auth/**", "/oauth2/**", "/login/**", "/error").permitAll()
								.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
								.requestMatchers("/actuator/**").permitAll().anyRequest().authenticated())
				.oauth2Login(oauth2 -> oauth2
						.authorizationEndpoint(auth -> auth.baseUri("/api/v1/auth/oauth2/authorization"))
						.redirectionEndpoint(red -> red.baseUri("/api/v1/auth/login/oauth2/code/*"))
						.userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
						.successHandler((request, response, authentication) -> {
							// Generate Token for the OAuth User
							String token = jwtService.generateToken(authentication.getName());
							// Redirect to frontend with token in URL
							response.sendRedirect(frontendUrl + "/oauth-success?token=" + token);
						}))
				.exceptionHandling(
						exception -> exception.authenticationEntryPoint((request, response, authException) -> {
							// Prevents 302 Redirect; sends 401 JSON for easier Frontend handling
							response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session expired or unauthorized");
						}));

		return http.build();
	}
}