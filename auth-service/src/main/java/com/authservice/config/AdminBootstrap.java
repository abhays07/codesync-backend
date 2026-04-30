package com.authservice.config;

import com.authservice.entity.Role;
import com.authservice.entity.User;
import com.authservice.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminBootstrap {

    @Bean
    public CommandLineRunner initAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByEmail("admin@codesync.com").isEmpty()) {
                User admin = new User();
                admin.setUsername("superadmin");
                admin.setEmail("admin@codesync.com");
                admin.setPasswordHash(passwordEncoder.encode("SecureAdmin@123")); 
                admin.setRole(Role.ADMIN);
                admin.setProvider("LOCAL");
                
                userRepository.save(admin);
                System.out.println("✅ Super Admin Account Created Successfully!");
            }
        };
    }
}
