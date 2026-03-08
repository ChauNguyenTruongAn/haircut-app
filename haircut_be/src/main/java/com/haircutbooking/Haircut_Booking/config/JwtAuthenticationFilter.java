package com.haircutbooking.Haircut_Booking.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.haircutbooking.Haircut_Booking.services.JWTService;
import com.haircutbooking.Haircut_Booking.services.UserService;

import io.jsonwebtoken.JwtException;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JWTService jwtService;
    private final UserService userService;

    @SuppressWarnings("null")
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String path = request.getRequestURI();
            logger.debug("Processing request for path: {}", path);

            // Bỏ qua xác thực cho các đường dẫn trong whitelist
            if (path.startsWith("/auth/") ||
                    path.startsWith("/swagger-ui") ||
                    path.startsWith("/v3/api-docs") ||
                    path.equals("/swagger-ui.html") ||
                    path.startsWith("/webhook/") ||
                    path.startsWith("/media/") ||
                    path.startsWith("/messenger/") ||
                    path.startsWith("/dialogflow/")) {

                logger.debug("Skipping authentication for whitelisted path: {}", path);
                filterChain.doFilter(request, response);
                return;
            }

            String header = request.getHeader("Authorization");
            String token = null;
            if (StringUtils.isNotEmpty(header) && header.startsWith("Bearer ")) {
                token = header.substring(7);
            }

            if (token != null && jwtService.validateToken(token)) {
                String username = jwtService.getUsernameFromToken(token);
                logger.debug("Authenticated user: {}", username);

                UserDetails userDetails = userService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);

        } catch (JwtException e) {
            logger.error("JWT validation failed", e);
            throw new JwtException("Token validation failed: " + e.getMessage(), e);
        }
    }

}
