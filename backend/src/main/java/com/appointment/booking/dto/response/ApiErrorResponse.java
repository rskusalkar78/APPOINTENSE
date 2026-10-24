package com.appointment.booking.dto.response;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        String timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> validationErrors
) {
    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(
                Instant.now().toString(),
                status,
                error,
                message,
                path,
                null
        );
    }

    public static ApiErrorResponse of(int status, String error, String message, String path, List<String> validationErrors) {
        return new ApiErrorResponse(
                Instant.now().toString(),
                status,
                error,
                message,
                path,
                validationErrors
        );
    }
}
