package com.apigateway.filter;

import com.apigateway.util.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    @Autowired
    private RouteValidator validator;

    @Autowired
    private JwtUtils jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (validator.isSecured.test(exchange.getRequest())) {
            // header contains token or not
            if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                logger.error("Missing authorization header for request: {}", exchange.getRequest().getURI());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                authHeader = authHeader.substring(7);
            } else {
                logger.error("Invalid authorization header format for request: {}", exchange.getRequest().getURI());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            try {
                // REST call to AUTH service - avoided!
                // Local validation
                jwtUtil.validateToken(authHeader);
                
                String username = jwtUtil.getUsernameFromToken(authHeader);
                String role = jwtUtil.getRoleFromToken(authHeader);
                
                // Enforce ADMIN access
                if (exchange.getRequest().getURI().getPath().startsWith("/api/v1/admin/")) {
                    if (role == null || !role.equalsIgnoreCase("ADMIN")) {
                        logger.error("Forbidden access to admin endpoint for user: {}", username);
                        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                        return exchange.getResponse().setComplete();
                    }
                }
                
                // Mutate the request to add user details in headers for downstream services
                exchange = exchange.mutate()
                    .request(exchange.getRequest().mutate()
                        .header("X-User-Name", username)
                        .header("X-User-Role", role)
                        .build())
                    .build();

            } catch (Exception e) {
                logger.error("Invalid access token for request: {}. Error: {}", exchange.getRequest().getURI(), e.getMessage());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1; // Ensure it runs early in the filter chain
    }
}
