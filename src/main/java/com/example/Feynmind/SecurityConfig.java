package com.example.Feynmind;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. Disable CSRF (Common cause of 403s for POST requests)
            .csrf(AbstractHttpConfigurer::disable)
            
            // 2. Enable CORS using the configuration source below
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 3. Define Access Rules
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll() // Allow Login/Signup
                .requestMatchers("/error").permitAll()       // Allow Error mappings
                .anyRequest().authenticated()                // Block everything else
            );
            
        return http.build();
    }

    // --- CORS CONFIGURATION (Allow React Frontend) ---
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow your Vite Frontend port
        configuration.setAllowedOrigins(List.of("http://localhost:5173")); 
        
        // Allow standard HTTP methods
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Allow all headers (needed for JWT tokens)
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        
        // Allow credentials (cookies/auth headers)
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}