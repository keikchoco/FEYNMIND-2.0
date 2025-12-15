package com.example.Feynmind.controller;

import com.example.Feynmind.model.User;
import com.example.Feynmind.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value; // Import this!
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173") 
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Reads "jwt.secret" from application.properties
    @Value("${jwt.secret}")
    private String secretKey;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is already in use!"));
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "User registered successfully!"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElse(null);

        if (user != null && passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            
            // Generate Key from the injected property
            Key key = Keys.hmacShaKeyFor(secretKey.getBytes());

            String token = Jwts.builder()
                    .setSubject(user.getEmail())
                    .setIssuedAt(new Date())
                    .setExpiration(new Date((new Date()).getTime() + 86400000)) 
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", Map.of("name", user.getName(), "email", user.getEmail()));
            
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
    }
}