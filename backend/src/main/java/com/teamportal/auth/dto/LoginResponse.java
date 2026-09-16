package com.teamportal.auth.dto;

import com.teamportal.user.dto.UserResponse;

/** Respuesta del login: { "token": "...", "user": { id, name, email, role, active } } */
public class LoginResponse {
    private String token;
    private UserResponse user;

    public LoginResponse(String token, UserResponse user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public UserResponse getUser() { return user; }
    public void setUser(UserResponse user) { this.user = user; }
}
