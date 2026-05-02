package com.example.demo.controller;

import com.example.demo.dtos.LoginRequest;
import com.example.demo.dtos.RegisterRequest;
import com.example.demo.dtos.TokenResponse;
import com.example.demo.dtos.UserDto;
import com.example.demo.entities.User;
import com.example.demo.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

//  Authentication endpoints:
// POST /api/v1/auth/register  - Naya account banao
// POST /api/v1/auth/login     - Login karo
// POST /api/v1/auth/refresh   - Access token refresh karo (cookie se)
// POST /api/v1/auth/logout    - Logout karo (cookie clear + token revoke)
// GET  /api/v1/auth/me        - Apni info dekho
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ==================== REGISTER ====================

    /**
     *  Naya user register karo
     * Request: { name, email, password }
     * Response: { accessToken, expiresIn, tokenType, user }
     * Cookie: refreshToken (HttpOnly)
     */
    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {

        TokenResponse tokenResponse = authService.register(request, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(tokenResponse);
    }

    // ==================== LOGIN ====================

    /**
     *  Email + Password se login karo
     * Request: { email, password }
     * Response: { accessToken, expiresIn, tokenType, user }
     * Cookie: refreshToken (HttpOnly)
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        TokenResponse tokenResponse = authService.login(request, response);
        return ResponseEntity.ok(tokenResponse);
    }

    // ==================== REFRESH TOKEN ====================

    /**
     *  Naya access token lo (refresh token cookie se automatic milega)
     * Cookie needed: refreshToken
     * Response: { accessToken, expiresIn, tokenType, user }
     * Note: Refresh token rotation hoti hai - naya refresh token cookie mein milega
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {

        TokenResponse tokenResponse = authService.refresh(request, response);
        return ResponseEntity.ok(tokenResponse);
    }

    // ==================== LOGOUT ====================

    /**
     *  Logout karo
     * - DB mein refresh token revoke ho jayega
     * - Cookie clear ho jayegi
     * Response: 204 No Content
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        authService.logout(request, response);
        return ResponseEntity.noContent().build();
    }

    // ==================== CURRENT USER ====================

    /**
     *  Currently logged in user ki info dekho
     * Header needed: Authorization: Bearer <accessToken>
     * Response: UserDto (without password)
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(
            @AuthenticationPrincipal User user) {

        UserDto userDto = authService.getCurrentUser(user.getEmail());
        return ResponseEntity.ok(userDto);
    }
}