package com.authservice.serviceImpl;

import com.authservice.entity.User;

import com.authservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. Fetch user details from the Provider (Google/GitHub)
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        // 2. Identify which provider was used
        String provider = userRequest.getClientRegistration().getRegistrationId().toUpperCase();
        
        // 3. Process the user data based on provider structure
        return processOAuth2User(oAuth2User, provider);
    }

    private OAuth2User processOAuth2User(OAuth2User oAuth2User, String provider) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        
        // GitHub sometimes hides email in the standard attribute map
        if (email == null && "GITHUB".equals(provider)) {
            email = (String) attributes.get("login") + "@github.com"; 
        }

        if (email == null) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {
            // User exists, update their details if necessary
            user = userOptional.get();
            user.setProvider(provider);
        } else {
            // New User: Auto-Register
            user = new User();
            user.setEmail(email);
            
            // Extract a clean username
            String name = (String) attributes.get("name");
            user.setFullName(name != null ? name : (String) attributes.get("login"));
            user.setUsername(email.split("@")[0] + "_" + provider.toLowerCase());
            
            user.setProvider(provider);
            user.setRole("DEVELOPER");
            user.setActive(true);
            user.setCreatedAt(LocalDateTime.now());
            // Social users don't have a local passwordHash
            user.setPasswordHash("OAUTH2_USER"); 
        }

        userRepository.save(user);
        return oAuth2User;
    }
}