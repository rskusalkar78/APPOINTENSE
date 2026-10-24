package com.appointment.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ServiceCreateRequest(
        @NotBlank(message = "Service name is required")
        @Size(min = 2, max = 120, message = "Service name must be between 2 and 120 characters")
        String name,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description,

        @NotNull(message = "Duration in minutes is required")
        @Positive(message = "Duration must be greater than 0")
        Integer durationMinutes,

        Boolean active
) {
}
