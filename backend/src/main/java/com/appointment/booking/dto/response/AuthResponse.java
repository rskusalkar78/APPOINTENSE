package com.appointment.booking.dto.response;

import com.appointment.booking.model.Role;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        Long userId,
        String email,
        String fullName,
        Role role
) {
    public static AuthResponse of(String token, long expiresIn, Long userId, String email, String fullName, Role role) {
        return new AuthResponse(
                token,
                "Bearer",
                expiresIn,
                userId,
                email,
                fullName,
                role
        );
    }
}
