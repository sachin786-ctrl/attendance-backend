package com.example.demo.security;

import com.example.demo.entities.Role;
import com.example.demo.entities.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// ✅ JWT tokens generate, parse aur validate karne ki service
@Service
@Getter
public class JwtService {

    private final SecretKey key;              // HMAC signing key
    private final long accessTtlSeconds;      // Access token expiry (default: 1 hour)
    private final long refreshTtlSeconds;     // Refresh token expiry (default: 7 days)
    private final String issuer;              // Token issuer claim

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.access-ttl-seconds}") long accessTtlSeconds,
            @Value("${security.jwt.refresh-ttl-seconds}") long refreshTtlSeconds,
            @Value("${security.jwt.issuer}") String issuer) {

        // ✅ Secret key minimum 64 characters honi chahiye for HS512
        if (secret == null || secret.length() < 64) {
            throw new IllegalArgumentException("JWT secret must be at least 64 characters long");
        }

        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
        this.issuer = issuer;
    }

    // ==================== Token Generation ====================

    /**
     * ✅ Access Token generate karo
     * - Short-lived (1 hour by default)
     * - Authorization header mein bhejo
     * - Claims mein email, roles aur type include hai
     */
    public String generateAccessToken(User user) {
        Instant now = Instant.now();

        // User ke roles ko string list mein convert karo
        List<String> roles = user.getRoles() == null
                ? List.of()
                : user.getRoles().stream().map(Role::name).toList();

        return Jwts.builder()
                .id(UUID.randomUUID().toString())          // Unique JWT ID
                .subject(user.getId().toString())          // User UUID as subject
                .issuer(issuer)                            // Who issued this token
                .issuedAt(Date.from(now))                  // Issue time
                .expiration(Date.from(now.plusSeconds(accessTtlSeconds))) // Expiry
                .claims(Map.of(                            // Custom claims
                        "email", user.getEmail(),
                        "roles", roles,
                        "typ", "access"                   // Token type marker
                ))
                .signWith(key, Jwts.SIG.HS512)            // HS512 signature
                .compact();
    }

    /**
     * ✅ Refresh Token generate karo
     * - Long-lived (7 days by default)
     * - HttpOnly cookie mein bhejo (XSS safe)
     * - jti DB mein store karke revoke kar sakte hain
     */
    public String generateRefreshToken(User user, String jti) {
        Instant now = Instant.now();

        return Jwts.builder()
                .id(jti)                                   // DB stored JTI for revocation
                .subject(user.getId().toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTtlSeconds)))
                .claim("typ", "refresh")                  // Refresh token marker
                .signWith(key, Jwts.SIG.HS512)
                .compact();
    }

    // ==================== Token Parsing & Validation ====================

    /**
     * ✅ Token parse karo - signature verify + expiry check
     * ExpiredJwtException ya JwtException throw kar sakta hai
     */
    public Jws<Claims> parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
    }

    /**
     * ✅ Ye access token hai ya nahi check karo
     */
    public boolean isAccessToken(String token) {
        Claims claims = parse(token).getPayload();
        return "access".equals(claims.get("typ"));
    }

    /**
     * ✅ Ye refresh token hai ya nahi check karo
     */
    public boolean isRefreshToken(String token) {
        Claims claims = parse(token).getPayload();
        return "refresh".equals(claims.get("typ"));
    }

    // ==================== Claims Extraction ====================

    public UUID getUserId(String token) {
        return UUID.fromString(parse(token).getPayload().getSubject());
    }

    public String getJti(String token) {
        return parse(token).getPayload().getId();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        return (List<String>) parse(token).getPayload().get("roles");
    }

    public String getEmail(String token) {
        return (String) parse(token).getPayload().get("email");
    }
}