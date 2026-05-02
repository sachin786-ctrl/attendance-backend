package com.example.demo.services;

import com.example.demo.dtos.RegisterRequest;
import com.example.demo.dtos.LoginRequest;
import com.example.demo.dtos.TokenResponse;
import com.example.demo.dtos.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    // ✅ Naya user register karo aur tokens return karo
    TokenResponse register(RegisterRequest request, HttpServletResponse response);

    // ✅ Login karo - access token return karo, refresh token cookie mein set karo
    TokenResponse login(LoginRequest request, HttpServletResponse response);

    // ✅ Refresh token se naya access token lo
    TokenResponse refresh(HttpServletRequest request, HttpServletResponse response);

    // ✅ Logout - refresh token revoke karo aur cookie clear karo
    void logout(HttpServletRequest request, HttpServletResponse response);

    // ✅ Currently logged in user ki info lo
    UserDto getCurrentUser(String email);
}