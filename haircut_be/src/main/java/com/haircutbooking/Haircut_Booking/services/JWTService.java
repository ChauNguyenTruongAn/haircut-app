package com.haircutbooking.Haircut_Booking.services;

import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JWTService {

    private final String secretKey;

    // Create a new key
    private final Key key;

    private final long accessTokenValidity = 1000 * 60 * 15;

    private final long refreshTokenValidity = 1000 * 60 * 60 * 24 * 7;

    public JWTService(@Value("${SPRING_SECRET_KEY}") String secretKey) {
        this.secretKey = secretKey;
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public String generateAccessToken(String username) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + accessTokenValidity);
        return Jwts.builder().setSubject(username).setIssuedAt(now).setExpiration(expireDate).signWith(key).compact();
    }

    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + refreshTokenValidity);
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expireDate)
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException ex) {
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser().setSigningKey(key).build().parseClaimsJws(token).getBody();
        return claims.getSubject();
    }
}
