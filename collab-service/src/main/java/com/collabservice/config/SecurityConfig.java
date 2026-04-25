package com.collabservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				// 1. MUST disable CSRF to allow POST requests from your React frontend
				.csrf(csrf -> csrf.disable())

				// 2. Ensure the session is stateless (standard for microservices)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				// 3. Configure permissions
				.authorizeHttpRequests(auth -> auth
						// Allow all routes starting with / for testing, or be specific:
						.requestMatchers("/sessions/**").permitAll().requestMatchers("/api/v1/sessions/**").permitAll()
						.requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll().anyRequest()
						.permitAll() // Temporarily permit all to bypass 403 during testing
				)

				// 4. Explicitly disable the UI features that cause redirects
				.formLogin(form -> form.disable()).httpBasic(basic -> basic.disable());

		return http.build();
	}
}