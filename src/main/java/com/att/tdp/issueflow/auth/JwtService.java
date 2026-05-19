package com.att.tdp.issueflow.auth;

import com.att.tdp.issueflow.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import io.jsonwebtoken.security.Keys;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties props;

    public String generateToken(User user) {
        Date now = new Date();
        return Jwts.builder()
            .subject(user.getUsername())
            .claim("userId", user.getId())
            .claim("role", user.getRole().name())
            .id(UUID.randomUUID().toString())
            .issuedAt(now)
            .expiration(new Date(now.getTime() + props.getExpirationMs()))
            .signWith(signingKey())
            .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractJti(String token) {
        return extractAllClaims(token).getId();
    }

    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
