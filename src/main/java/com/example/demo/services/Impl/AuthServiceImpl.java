package com.example.demo.services.Impl;

import com.example.demo.dtos.*;
import com.example.demo.entities.Provider;
import com.example.demo.entities.RefreshToken;
import com.example.demo.entities.Role;
import com.example.demo.entities.User;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.RefreshTokenRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.security.CookieService;
import com.example.demo.security.JwtService;
import com.example.demo.services.AuthService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final CookieService cookieService;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    // ==================== REGISTER ====================

    @Override
    public TokenResponse register(RegisterRequest request, HttpServletResponse response) {
        logger.info("Registering new user: {}", request.email());

        // ✅ Step 1: Email normalize karo (lowercase + trim)
        String email = request.email().trim().toLowerCase();

        // ✅ Step 2: Duplicate email check
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered: " + email);
        }

        // ✅ Step 3: User entity banao
        User user = User.builder()
                .name(request.name().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password())) // ✅ BCrypt encode
                .enable(true)
                .provider(Provider.LOCAL)
                .roles(new HashSet<>(Set.of(Role.USER)))
                .build();

        // ✅ Step 4: DB mein save karo
        User savedUser = userRepository.save(user);
        logger.info("User registered successfully: {}", savedUser.getId());

        // ✅ Step 5: Tokens generate karo aur return karo
        return generateAndAttachTokens(savedUser, response);
    }

    // ==================== LOGIN ====================

    @Override
    public TokenResponse login(LoginRequest request, HttpServletResponse response) {
        logger.info("Login attempt for: {}", request.email());

        // ✅ Step 1: Email normalize karo
        String email = request.email().trim().toLowerCase();

        // ✅ Step 2: User dhundo
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        // ✅ Step 3: Account enabled check
        if (!user.isEnabled()) {
            throw new BadCredentialsException("Account is disabled");
        }

        // ✅ Step 4: Password verify karo
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        logger.info("Login successful for: {}", user.getId());

        // ✅ Step 5: Tokens generate karo
        return generateAndAttachTokens(user, response);
    }

    // ==================== REFRESH ====================

    @Override
    public TokenResponse refresh(HttpServletRequest request, HttpServletResponse response) {

        // ✅ Step 1: Cookie se refresh token nikalo
        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken == null) {
            throw new BadCredentialsException("Refresh token not found");
        }

        // ✅ Step 2: Token parse karo (signature + expiry check)
        Claims claims;
        try {
            claims = jwtService.parse(refreshToken).getPayload();
        } catch (ExpiredJwtException e) {
            cookieService.clearRefreshCookie(response); // Expired cookie clear karo
            throw new BadCredentialsException("Refresh token expired, please login again");
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        // ✅ Step 3: Token type check
        if (!"refresh".equals(claims.get("typ"))) {
            throw new BadCredentialsException("Invalid token type");
        }

        // ✅ Step 4: DB mein JTI check karo (revoked ya nahi)
        String jti = claims.getId();
        RefreshToken storedToken = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> new BadCredentialsException("Refresh token not recognized"));

        if (storedToken.isRevoked()) {
            // ✅ Revoked token use hua - possible token theft! Saare tokens revoke karo
            logger.warn("Revoked refresh token used! Possible token theft for user: {}",
                    storedToken.getUser().getId());
            refreshTokenRepository.deleteAllByUser(storedToken.getUser());
            cookieService.clearRefreshCookie(response);
            throw new BadCredentialsException("Token reuse detected. Please login again");
        }

        // ✅ Step 5: Expiry double-check (DB se)
        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            cookieService.clearRefreshCookie(response);
            throw new BadCredentialsException("Refresh token expired");
        }

        // ✅ Step 6: Purana token revoke karo (Token Rotation)
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        // ✅ Step 7: Naya token generate karo
        User user = storedToken.getUser();
        return generateAndAttachTokens(user, response);
    }

    // ==================== LOGOUT ====================

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {

        // ✅ Cookie se refresh token nikalo
        String refreshToken = extractRefreshTokenFromCookie(request);

        if (refreshToken != null) {
            try {
                String jti = jwtService.getJti(refreshToken);
                // ✅ DB mein token revoke karo
                refreshTokenRepository.findByJti(jti).ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                    logger.info("Refresh token revoked for user: {}", token.getUser().getId());
                });
            } catch (Exception e) {
                // Token invalid hai - koi baat nahi, cookie to clear karenge hi
                logger.debug("Could not parse refresh token during logout: {}", e.getMessage());
            }
        }

        // ✅ Cookie hamesha clear karo, chahe token valid ho ya na ho
        cookieService.clearRefreshCookie(response);
        cookieService.addNoStoreHeaders(response);
    }

    // ==================== CURRENT USER ====================

    @Override
    @Transactional(readOnly = true)
    public UserDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // ✅ Password ko response mein null karo
        UserDto dto = modelMapper.map(user, UserDto.class);
        dto.setPassword(null);
        return dto;
    }

    // ==================== PRIVATE HELPERS ====================

    /**
     * ✅ User ke liye access + refresh token generate karo
     * - Access token: response body mein
     * - Refresh token: HttpOnly cookie mein
     */
    private TokenResponse generateAndAttachTokens(User user, HttpServletResponse response) {

        // ✅ Naya JTI (JWT ID) banao aur DB mein store karo
        String jti = UUID.randomUUID().toString();
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .jti(jti)
                .user(user)
                .revoked(false)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        // ✅ Tokens generate karo
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user, jti);

        // ✅ Refresh token HttpOnly cookie mein attach karo
        cookieService.attachRefreshCookie(response, refreshToken,
                (int) jwtService.getRefreshTtlSeconds());
        cookieService.addNoStoreHeaders(response);

        // ✅ User DTO banao (password null karke)
        UserDto userDto = modelMapper.map(user, UserDto.class);
        userDto.setPassword(null);

        // ✅ Access token + user info response mein return karo
        return TokenResponse.of(accessToken, refreshToken, jwtService.getAccessTtlSeconds(), userDto);
    }

    /**
     * ✅ HttpServletRequest ki cookies se refresh token nikalo
     */
    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        String cookieName = cookieService.getRefreshTokenCookieName();
        return Arrays.stream(cookies)
                .filter(c -> cookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}