package com.volunteer.auth.service;

import com.volunteer.auth.model.User;
import com.volunteer.auth.model.AuthResponse;
import com.volunteer.auth.model.UserType;
import com.volunteer.auth.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
@Transactional
public class AuthService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated");
        }

        String token = generateToken(user);
        return createAuthResponse(user, token);
    }

    public AuthResponse register(User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setActive(true);
        User savedUser = userRepository.save(user);
        String token = generateToken(savedUser);
        return createAuthResponse(savedUser, token);
    }

    private String generateToken(User user) {
        byte[] keyBytes = jwtSecret.getBytes();
        Key key = Keys.hmacShaKeyFor(keyBytes);

        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("userType", user.getUserType())
                .claim("name", user.getName())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    private AuthResponse createAuthResponse(User user, String token) {
        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getUserType(),
                token
        );
    }
}