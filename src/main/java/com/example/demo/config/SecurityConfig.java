package com.example.demo.config;

import com.example.demo.dtos.ApiError;
import com.example.demo.security.JwtAuthenticationFilter;
import com.example.demo.security.OAuth2SuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // ✅ @PreAuthorize, @PostAuthorize enable karo

public class SecurityConfig {
    @Value("${app.cors.front-end-url}")
    private String frontendUrls;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          OAuth2SuccessHandler oAuth2SuccessHandler,
                          ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // ✅ CSRF disable - JWT stateless app mein CSRF ki zarurat nahi
                .csrf(AbstractHttpConfigurer::disable)

                // ✅ CORS enable - frontend se cross-origin requests allow karo
                .cors(Customizer.withDefaults())

                // ✅ Stateless session - JWT app mein server-side session nahi chahiye
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ==================== URL Authorization ====================
                .authorizeHttpRequests(auth -> auth
                        // ✅ Public endpoints - koi bhi access kar sakta hai
                        .requestMatchers(AppConstants.AUTH_PUBLIC_URLS).permitAll()

                        // ✅ OPTIONS preflight requests allow karo (CORS ke liye)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ✅ Admin-only endpoints
                        .requestMatchers(AppConstants.AUTH_ADMIN_URLS)
                        .hasRole(AppConstants.ADMIN_ROLE)

                        // ✅ Guest endpoints
                        .requestMatchers(AppConstants.AUTH_GUEST_URLS)
                        .hasRole(AppConstants.GUEST_ROLE)

                        // ✅ Baaki sabhi endpoints ke liye authentication required
                        .anyRequest().authenticated()
                )

                // ==================== OAuth2 Login ====================
                .oauth2Login(oauth2 -> oauth2
                        // ✅ OAuth2 success pe JWT tokens generate karega
                        .successHandler(oAuth2SuccessHandler)
                        // ✅ OAuth2 failure pe error page
                        .failureHandler((request, response, exception) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json");
                            var error = ApiError.of(401, "OAuth2 Login Failed",
                                    exception.getMessage(), request.getRequestURI());
                            response.getWriter().write(objectMapper.writeValueAsString(error));
                        })
                )

                // ✅ Default form login disable karo (hum JWT use kar rahe hain)
                .logout(AbstractHttpConfigurer::disable)

                // ==================== Exception Handling ====================
                .exceptionHandling(ex -> ex

                        // ✅ 401 - Authentication nahi hai (invalid/missing token)
                        .authenticationEntryPoint((request, response, e) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json");

                            // Filter ne jo error set kiya tha wo use karo (Expired, Invalid etc.)
                            String message = (String) request.getAttribute("error");
                            if (message == null) message = "Authentication required";

                            var error = ApiError.of(401, "Unauthorized", message,
                                    request.getRequestURI());
                            response.getWriter().write(objectMapper.writeValueAsString(error));
                        })

                        // ✅ 403 - Authenticated hai but permission nahi
                        .accessDeniedHandler((request, response, e) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json");

                            var error = ApiError.of(403, "Forbidden",
                                    "You don't have permission to access this resource",
                                    request.getRequestURI());
                            response.getWriter().write(objectMapper.writeValueAsString(error));
                        })
                )

                // ✅ JWT filter ko UsernamePasswordAuthenticationFilter se PEHLE add karo
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ✅ Password encoder - BCrypt (industry standard, slow by design)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ✅ AuthenticationManager - AuthService mein login ke liye use hoga
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // ✅ CORS configuration - frontend domains allow karo
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.front-end-url}") String corsUrls) {

        // ✅ Comma-separated URLs ko array mein convert karo
        String[] urls = Arrays.stream(corsUrls.split(","))
                .map(String::trim)
                .toArray(String[]::new);

        var config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(urls));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
        config.setAllowedHeaders(List.of("*"));

        // ✅ Credentials allow karo (cookies + Authorization header ke liye zaruri)
        config.setAllowCredentials(true);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}