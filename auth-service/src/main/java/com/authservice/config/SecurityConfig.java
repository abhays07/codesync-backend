package com.authservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import com.authservice.serviceImpl.CustomOAuth2UserService;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Security Configuration - Centralizes Auth Rules Standardizes Password
 * Encoding and OAuth2 Integration
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Autowired
	private CustomOAuth2UserService customOAuth2UserService;

	@Bean
	public PasswordEncoder passwordEncoder() {
		// Industry Standard: BCrypt for one-way password hashing
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth
				// Public Endpoints
				.requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/oauth2/**", "/login/**").permitAll()
				.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
				.requestMatchers("/actuator/**").permitAll()
				// Secured Endpoints
				.anyRequest().authenticated())
				.oauth2Login(
						oauth2 -> oauth2.userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
								// Frontend callback after Google/GitHub login
								.defaultSuccessUrl("http://localhost:5173/oauth-success", true))
				.exceptionHandling(
						exception -> exception.authenticationEntryPoint((request, response, authException) -> {
							// Prevents 302 Redirect; sends 401 JSON for easier Frontend handling
							response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session expired or unauthorized");
						}));

		return http.build();
	}
}