// AuthService.java
package com.volunteer.auth.service;

import com.volunteer.auth.model.AuthResponse;
import com.volunteer.auth.model.User;
import com.volunteer.auth.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Autowired
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse register(User user) {
        logger.debug("Attempting to register user with email: {}", user.getEmail());

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            logger.debug("Registration failed - Email already exists: {}", user.getEmail());
            throw new RuntimeException("Email déjà utilisé");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setActive(true);
        User savedUser = userRepository.save(user);
        logger.debug("User registered successfully with ID: {}", savedUser.getId());

        String token = generateToken(savedUser);
        return new AuthResponse(savedUser.getId(), savedUser.getEmail(),
                savedUser.getName(), savedUser.getUserType(), token);
    }

    public AuthResponse login(String email, String password) {
        logger.debug("Attempting login for email: {}", email);

        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            logger.debug("Login failed - User not found with email: {}", email);
            throw new RuntimeException("Email ou mot de passe incorrect");
        }

        User user = userOptional.get();

        if (!user.isActive()) {
            logger.debug("Login failed - Account is inactive for email: {}", email);
            throw new RuntimeException("Compte désactivé");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            logger.debug("Login failed - Invalid password for email: {}", email);
            throw new RuntimeException("Email ou mot de passe incorrect");
        }

        logger.debug("Login successful for user ID: {}", user.getId());
        String token = generateToken(user);
        return new AuthResponse(user.getId(), user.getEmail(),
                user.getName(), user.getUserType(), token);
    }

    private String generateToken(User user) {
        Date now = new Date();
        byte[] decodedKey = Decoders.BASE64.decode(jwtSecret);
        SecretKey key = Keys.hmacShaKeyFor(decodedKey);

        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("userType", user.getUserType())
                .claim("name", user.getName())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + jwtExpiration))
                .signWith(key)
                .compact();
    }
}