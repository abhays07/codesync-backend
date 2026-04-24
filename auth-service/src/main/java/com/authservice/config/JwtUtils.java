package com.authservice.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtils {

    // Injects the value from JWT_SECRET in your .env via application.yml
    @Value("${JWT_SECRET}")
    private String jwtSecret;

    // Injects the value from TOKEN_EXPIRY in your .env (defaulting to 24h if missing) 
    @Value("${TOKEN_EXPIRY:86400000}")
    private long tokenExpiry;

    private Key getSigningKey() {
        // Using getBytes() as per your original logic, 
        // but ensure your secret is long enough for HS256 (32+ characters)
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + tokenExpiry))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Log this in a real scenario to see if it's expired or malformed
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}