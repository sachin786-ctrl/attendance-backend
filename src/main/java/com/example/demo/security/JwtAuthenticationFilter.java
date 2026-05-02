package com.example.demo.security;

import com.example.demo.Helper.UserHelper;
import com.example.demo.repositories.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// ✅ Har HTTP request pe ek baar chalega (OncePerRequestFilter)
// Authorization header se JWT token validate karta hai
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        // ✅ Authorization header nahi hai ya "Bearer " se start nahi hota
        // Token nahi to filter skip karo - Spring Security baad mein 401 dega agar route protected hai
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // ✅ "Bearer " ke baad wala part nikalo (7 characters skip)
        String token = header.substring(7);

        try {
            // ✅ Sirf access tokens allow karo (refresh token se API call nahi honi chahiye)
            if (!jwtService.isAccessToken(token)) {
                request.setAttribute("error", "Invalid token type. Use access token.");
                filterChain.doFilter(request, response);
                return;
            }

            // ✅ Token parse karo - signature verify + expiry check
            Jws<Claims> parsedToken = jwtService.parse(token);
            Claims payload = parsedToken.getPayload();

            // ✅ Subject se user UUID nikalo
            String userId = payload.getSubject();
            UUID userUuid = UserHelper.parseUUID(userId);

            // ✅ Security context mein already authentication hai to dobara set mat karo
            if (SecurityContextHolder.getContext().getAuthentication() == null) {

                // ✅ DB se user fetch karo (enabled check + fresh roles ke liye)
                userRepository.findById(userUuid).ifPresent(user -> {

                    // ✅ Disabled user ka token valid nahi
                    if (!user.isEnabled()) {
                        request.setAttribute("error", "Account is disabled");
                        return;
                    }

                    // ✅ User ke roles se GrantedAuthority list banao
                    List<GrantedAuthority> authorities = user.getRoles() == null
                            ? List.of()
                            : user.getRoles().stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                            .collect(Collectors.toList());

                    // ✅ Authentication object banao
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    user,  // Principal (username)
                                    null,             // Credentials (password - null for token auth)
                                    authorities       // Roles
                            );

                    // ✅ Request details attach karo (IP address etc.)
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // ✅ Security context mein set karo - ab request authenticated hai
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.debug("Authenticated user: {} for URI: {}", user.getEmail(), request.getRequestURI());
                });
            }

        } catch (ExpiredJwtException e) {
            // ✅ Token expire ho gaya - error attribute set karo (SecurityConfig mein use hoga)
            logger.debug("JWT token expired: {}", e.getMessage());
            request.setAttribute("error", "Token expired");

        } catch (Exception e) {
            // ✅ Invalid signature, malformed token etc.
            logger.debug("JWT token invalid: {}", e.getMessage());
            request.setAttribute("error", "Invalid token");
        }

        // ✅ Hamesha filter chain continue karo
        // Agar authentication set nahi hua to Spring Security 401 dega
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // ✅ Auth endpoints pe ye filter skip karo (login, register ke liye token nahi chahiye)
        String path = request.getRequestURI();

        return path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/register")
                || path.equals("/api/v1/auth/refresh");
    }
}