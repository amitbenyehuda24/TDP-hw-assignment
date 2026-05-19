package com.att.tdp.issueflow.auth;

import com.att.tdp.issueflow.auth.dto.AuthResponse;
import com.att.tdp.issueflow.auth.dto.LoginRequest;
import com.att.tdp.issueflow.user.UserService;
import com.att.tdp.issueflow.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader("Authorization") String authHeader) {
        authService.logout(authHeader);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return userService.findById(userId);
    }
}
