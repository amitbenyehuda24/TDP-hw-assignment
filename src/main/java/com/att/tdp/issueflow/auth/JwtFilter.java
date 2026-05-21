package com.att.tdp.issueflow.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenBlocklistRepository tokenBlocklistRepository;

    @Autowired
    public JwtFilter(JwtService jwtService, @Lazy TokenBlocklistRepository tokenBlocklistRepository) {
        this.jwtService = jwtService;
        this.tokenBlocklistRepository = tokenBlocklistRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        if (!jwtService.isTokenValid(token)) {
            chain.doFilter(request, response);
            return;
        }

        // Reject tokens that have been explicitly logged out
        String jti = jwtService.extractJti(token);
        if (tokenBlocklistRepository.existsByJti(jti)) {
            chain.doFilter(request, response);
            return;
        }

        Long userId = jwtService.extractUserId(token);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            userId, null, Collections.emptyList()
        );
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);

        chain.doFilter(request, response);
    }
}
