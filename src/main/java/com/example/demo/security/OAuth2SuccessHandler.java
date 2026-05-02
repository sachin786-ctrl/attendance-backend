package com.example.demo.security;

import com.example.demo.entities.Provider;
import com.example.demo.entities.RefreshToken;
import com.example.demo.entities.Role;
import com.example.demo.entities.User;
import com.example.demo.repositories.RefreshTokenRepository;
import com.example.demo.repositories.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Google/GitHub se successful login hone ke baad ye handler chalega
// Naya user create karega (ya existing dhundhega) aur JWT tokens generate karega
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final Logger logger = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final CookieService cookieService;

    // Successful login ke baad frontend pe redirect karna hai
    @Value("${app.auth.frontend.success-redirect}")
    private String frontendSuccessUrl;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        logger.info("OAuth2 authentication successful");
        logger.info(authentication.toString());

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // Kaun sa provider hai (google ya github)
        String registrationId = "unknown";
        if (authentication instanceof OAuth2AuthenticationToken token) {
            registrationId = token.getAuthorizedClientRegistrationId();
        }
        logger.info("OAuth2 provider: {}", registrationId);

        // Provider ke hisaab se user data extract karo
        User user;
        switch (registrationId) {
            case "google":
                user = handleGoogleLogin(attributes);
                break;
            case "github":
                user = handleGithubLogin(attributes);
                break;
            default:
                throw new RuntimeException("Unsupported OAuth2 provider: " + registrationId);
        }

        // Refresh token DB mein save karo
        String jti = UUID.randomUUID().toString();
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .jti(jti)
                .user(user)
                .revoked(false)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        // Tokens generate karo
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user, jti);

        // Refresh token HttpOnly cookie mein set karo
        cookieService.attachRefreshCookie(response, refreshToken,
                (int) jwtService.getRefreshTtlSeconds());

        // Access token URL parameter mein daal ke frontend pe redirect karo
        // Frontend is token ko localStorage/memory mein store karega
        String redirectUrl = frontendSuccessUrl + "?token=" + accessToken;
        logger.info("Redirecting to frontend: {}", frontendSuccessUrl);
        response.sendRedirect(redirectUrl);
    }

    // ==================== PRIVATE: Provider Handlers ====================

    /**
     * Google se aaye user ko handle karo
     * Google ke attributes: sub (id), email, name, picture
     */
    private User handleGoogleLogin(Map<String, Object> attributes) {
        String googleId = attributes.getOrDefault("sub", "").toString();
        String email = attributes.getOrDefault("email", "").toString().toLowerCase();
        String name = attributes.getOrDefault("name", "").toString();
        String picture = attributes.getOrDefault("picture", "").toString();

        logger.info("Google login for email: {}", email);

        // Existing user dhundo, nahi mila to naya banao
        return userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                    .email(email)
                    .name(name)
                    .image(picture)
                    .enable(true)
                    .provider(Provider.GOOGLE)
                    .providerId(googleId)
                    .roles(new HashSet<>(Set.of(Role.USER)))
                    .build();
            User saved = userRepository.save(newUser);
            logger.info("New Google user created: {}", saved.getId());
            return saved;
        });
    }

    /**
     * GitHub se aaye user ko handle karo
     * GitHub ke attributes: id, login (username), avatar_url, email (nullable)
     */
    private User handleGithubLogin(Map<String, Object> attributes) {
        String githubId = attributes.getOrDefault("id", "").toString();
        String name = attributes.getOrDefault("login", "").toString();
        String image = attributes.getOrDefault("avatar_url", "").toString();

        // GitHub kabhi kabhi email nahi deta (private email settings)
        // Fallback: username@github.com
        String email = (String) attributes.get("email");
        if (email == null || email.isBlank()) {
            email = name + "@github.com";
        }
        email = email.toLowerCase();

        logger.info("GitHub login for: {}", name);

        final String finalEmail = email;
        return userRepository.findByEmail(finalEmail).orElseGet(() -> {
            User newUser = User.builder()
                    .email(finalEmail)
                    .name(name)
                    .image(image)
                    .enable(true)
                    .provider(Provider.GITHUB)
                    .providerId(githubId)
                    .roles(new HashSet<>(Set.of(Role.USER)))
                    .build();
            User saved = userRepository.save(newUser);
            logger.info("New GitHub user created: {}", saved.getId());
            return saved;
        });
    }
}