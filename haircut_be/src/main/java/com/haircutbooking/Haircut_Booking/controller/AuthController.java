package com.haircutbooking.Haircut_Booking.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.haircutbooking.Haircut_Booking.domain.User;
import com.haircutbooking.Haircut_Booking.domain.RequestDTO.AuthRequest;
import com.haircutbooking.Haircut_Booking.domain.RequestDTO.RegisterRequest;
import com.haircutbooking.Haircut_Booking.services.JWTService;
import com.haircutbooking.Haircut_Booking.services.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import com.haircutbooking.Haircut_Booking.repositories.UserRepository;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final JWTService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        logger.info("Login request received for user: {}", authRequest.getUsername());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String accessToken = jwtService.generateAccessToken(authRequest.getUsername());
            String refreshToken = jwtService.generateRefreshToken(authRequest.getUsername());

            // Tạo response bao gồm token và thông tin user
            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken);

            // Lấy thông tin user để trả về client
            try {
                User user = userRepository.findByUsername(authRequest.getUsername())
                        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("username", user.getUsername());
                userMap.put("email", user.getEmail());
                userMap.put("name", user.getFullName());
                userMap.put("role", user.getRole().getRoleName());

                response.put("user", userMap);
            } catch (Exception e) {
                logger.warn("Could not load user details for response: {}", e.getMessage());
                // Vẫn trả về token nếu không lấy được thông tin user
            }

            logger.info("Login successful for user: {}", authRequest.getUsername());
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            // Lỗi do sai username/password
            logger.warn("Invalid credentials for user: {}", authRequest.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        } catch (Exception e) {
            // Lỗi tổng quát
            logger.error("Error during login for user: {}", authRequest.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (jwtService.validateToken(refreshToken)) {
            String username = jwtService.getUsernameFromToken(refreshToken);

            String newAccessToken = jwtService.generateAccessToken(username);
            String newRefreshToken = jwtService.generateRefreshToken(username);
            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", newAccessToken);
            tokens.put("refreshToken", newRefreshToken);

            return ResponseEntity.ok(tokens);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        request.setPassword(passwordEncoder.encode(request.getPassword()));
        User user = userService.addUser(request);
        if (user != null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid input");
    }

    @GetMapping()
    public String getMethodName() {
        return new String("hello forom faf");
    }

}
