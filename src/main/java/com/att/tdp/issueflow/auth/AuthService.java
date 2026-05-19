package com.att.tdp.issueflow.auth;

import com.att.tdp.issueflow.auth.dto.AuthResponse;
import com.att.tdp.issueflow.auth.dto.LoginRequest;
import com.att.tdp.issueflow.common.exception.UnauthorizedException;
import com.att.tdp.issueflow.user.User;
import com.att.tdp.issueflow.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlocklistRepository tokenBlocklistRepository;

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.getUsername())
            .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid username or password");
        }

        return new AuthResponse(jwtService.generateToken(user));
    }

    public void logout(String authHeader) {
        String token = extractBearer(authHeader);

        if (!jwtService.isTokenValid(token)) {
            throw new UnauthorizedException("Token is invalid or expired");
        }

        String jti = jwtService.extractJti(token);

        if (tokenBlocklistRepository.existsByJti(jti)) {
            return; // already logged out, idempotent
        }

        TokenBlocklist entry = new TokenBlocklist();
        entry.setJti(jti);
        entry.setExpiresAt(jwtService.extractExpiration(token)
            .toInstant().atOffset(ZoneOffset.UTC));
        tokenBlocklistRepository.save(entry);
    }

    private String extractBearer(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or malformed Authorization header");
        }
        return authHeader.substring(7);
    }
}
