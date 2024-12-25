package com.volunteer.auth.model;

public class AuthResponse {
    private final Integer userId;
    private final String email;
    private final String name;
    private final UserType userType;
    private final String token;

    public AuthResponse(Integer userId, String email, String name, UserType userType, String token) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.userType = userType;
        this.token = token;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public UserType getUserType() {
        return userType;
    }

    public String getToken() {
        return token;
    }
}