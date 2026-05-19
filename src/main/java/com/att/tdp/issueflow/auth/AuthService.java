package com.att.tdp.issueflow.auth;

import com.att.tdp.issueflow.auth.dto.AuthResponse;
import com.att.tdp.issueflow.auth.dto.LoginRequest;
import com.att.tdp.issueflow.common.exception.UnauthorizedException;
import com.att.tdp.issueflow.user.User;
import com.att.tdp.issueflow.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.getUsername())
            .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid username or password");
        }

        return new AuthResponse(jwtService.generateToken(user));
    }
}
