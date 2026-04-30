package com.paymentservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthFilter;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
		this.jwtAuthFilter = jwtAuthFilter;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(
						auth -> auth
								.requestMatchers("/actuator/**").permitAll()
								.requestMatchers("/api/v1/payments/**").permitAll()
								.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
								.anyRequest().authenticated())
				.formLogin(form -> form.disable()).httpBasic(basic -> basic.disable())
				.addFilterBefore(jwtAuthFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}
}