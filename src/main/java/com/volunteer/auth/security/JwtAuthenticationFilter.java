package com.volunteer.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.ArrayList;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_HEADER = "X-User-Id";

    private final String jwtSecret;

    public JwtAuthenticationFilter(String jwtSecret) {
        this.jwtSecret = jwtSecret;
        logger.debug("JwtAuthenticationFilter initialized with secret key length: {}", jwtSecret.length());
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = extractToken(request);
            logger.debug("Processing request to: {}", request.getRequestURI());

            if (token != null) {
                logger.debug("Found JWT token");
                Claims claims = validateToken(token);
                String userId = claims.getSubject();
                String userType = claims.get("userType", String.class);

                logger.debug("JWT validated for user ID: {}, type: {}", userId, userType);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, new ArrayList<>());

                SecurityContextHolder.getContext().setAuthentication(authentication);
                request.setAttribute(USER_ID_HEADER, userId);
                response.setHeader(USER_ID_HEADER, userId);

                logger.debug("Authentication set in SecurityContext");
                filterChain.doFilter(request, response);
            } else {
                logger.warn("No JWT token found in request");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }

        } catch (Exception e) {
            logger.error("Error processing request: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            logger.debug("Extracted token: {}", token.substring(0, Math.min(token.length(), 10)) + "...");
            return token;
        }
        return null;
    }

    private Claims validateToken(String token) throws ServletException {
        try {
            byte[] decodedKey = Decoders.BASE64.decode(jwtSecret);
            SecretKey key = Keys.hmacShaKeyFor(decodedKey);
            logger.debug("Validating token with secret key length: {}", decodedKey.length);

            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            throw new ServletException("Invalid JWT token: " + e.getMessage());
        }
    }
}