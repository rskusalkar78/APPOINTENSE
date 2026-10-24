package com.appointment.booking.service.impl;

import com.appointment.booking.dto.request.UserLoginRequest;
import com.appointment.booking.dto.response.AuthResponse;
import com.appointment.booking.security.JwtProvider;
import com.appointment.booking.security.UserPrincipal;
import com.appointment.booking.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;

    public AuthServiceImpl(AuthenticationManager authenticationManager, JwtProvider jwtProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtProvider = jwtProvider;
    }

    @Override
    public AuthResponse login(UserLoginRequest request) {
        if (request == null || request.email() == null || request.password() == null) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtProvider.generateToken(principal.getId(), principal.getUsername(), principal.getRole());

        return AuthResponse.of(
                token,
                jwtProvider.getExpirationMillis(),
                principal.getId(),
                principal.getUsername(),
                principal.getFullName(),
                principal.getRole()
        );
    }
}
