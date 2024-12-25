package com.volunteer.auth.controller;

import com.volunteer.auth.model.AuthResponse;
import com.volunteer.auth.model.LoginRequest;
import com.volunteer.auth.model.User;
import com.volunteer.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            AuthResponse response = authService.login(loginRequest.getEmail(), loginRequest.getPassword());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            AuthResponse response = authService.register(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
}

class ErrorResponse {
    private final String message;

    public ErrorResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}