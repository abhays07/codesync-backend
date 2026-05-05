package com.projectservice.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        System.out.println("=== PROJECT-SERVICE DEBUG ===");
        System.out.println("Secret length: " + (jwtSecret != null ? jwtSecret.length() : "null"));
        System.out.println("Secret prefix: " + (jwtSecret != null && jwtSecret.length() > 10 ? jwtSecret.substring(0, 10) : "short"));
        System.out.println("=============================");
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(jwtSecret.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8))
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                String username = claims.getSubject();
                String role = claims.get("role", String.class);

                if (role != null) {
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
                    UsernamePasswordAuthenticationToken auth = 
                        new UsernamePasswordAuthenticationToken(username, null, Collections.singletonList(authority));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    System.out.println("JWT Validated successfully for user: " + username + " with role: ROLE_" + role);
                } else {
                    System.out.println("JWT Validation: No role found in token for user: " + username);
                }
            } catch (Exception e) {
                System.err.println("JWT Validation FAILED: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            if (request.getRequestURI().startsWith("/api/v1/admin/")) {
                System.out.println("JWT Validation: No Bearer token found in request to " + request.getRequestURI());
            }
        }
        filterChain.doFilter(request, response);
    }
}
