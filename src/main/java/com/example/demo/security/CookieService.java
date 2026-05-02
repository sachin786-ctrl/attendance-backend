package com.example.demo.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

// ✅ Refresh token ko HttpOnly cookie mein manage karne ke liye
// HttpOnly = JavaScript se access nahi hoga (XSS attack se safe)
@Service
@Getter
public class CookieService {

    private final Logger logger = LoggerFactory.getLogger(CookieService.class);

    private final String refreshTokenCookieName;
    private final boolean cookieHttpOnly;
    private final boolean cookieSecure;
    private final String cookieDomain;
    private final String cookieSameSite;

    public CookieService(
            @Value("${security.jwt.refresh-token-cookie-name}") String refreshTokenCookieName,
            @Value("${security.jwt.cookie-http-only}") boolean cookieHttpOnly,
            @Value("${security.jwt.cookie-secure}") boolean cookieSecure,
            @Value("${security.jwt.cookie-same-site}") String cookieSameSite,
            @Value("${security.jwt.cookie-domain}") String cookieDomain) {

        this.refreshTokenCookieName = refreshTokenCookieName;
        this.cookieHttpOnly = cookieHttpOnly;
        this.cookieSecure = cookieSecure;
        this.cookieDomain = cookieDomain;
        this.cookieSameSite = cookieSameSite;
    }

    /**
     * ✅ Response mein refresh token cookie attach karo
     * @param response  HTTP response
     * @param value     Refresh token string
     * @param maxAge    Cookie expiry (seconds mein)
     */
    public void attachRefreshCookie(HttpServletResponse response, String value, int maxAge) {
        logger.debug("Attaching refresh cookie, maxAge={}s", maxAge);

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(refreshTokenCookieName, value)
                .httpOnly(cookieHttpOnly)   // JS access nahi hoga
                .secure(cookieSecure)       // HTTPS only (prod mein true)
                .path("/")                  // Sab paths pe available
                .maxAge(maxAge)             // Expiry time
                .sameSite(cookieSameSite);  // CSRF protection

        // ✅ Domain sirf tab set karo jab explicitly configure kiya ho
        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    /**
     * ✅ Refresh token cookie clear karo (logout ke liye)
     * maxAge=0 se browser cookie immediately delete kar deta hai
     */
    public void clearRefreshCookie(HttpServletResponse response) {
        logger.debug("Clearing refresh cookie");

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(refreshTokenCookieName, "")
                .httpOnly(cookieHttpOnly)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)              // ✅ maxAge=0 = cookie delete
                .sameSite(cookieSameSite);

        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    /**
     * ✅ Cache-Control headers set karo taaki browser token cache na kare
     */
    public void addNoStoreHeaders(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader("Pragma", "no-cache");
    }
}