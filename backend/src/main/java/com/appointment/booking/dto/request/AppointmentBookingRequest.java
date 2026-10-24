package com.appointment.booking.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AppointmentBookingRequest(
        // When called via REST API, userId is securely derived from JWT principal by AppointmentController
        Long userId,

        @NotNull(message = "Appointment slot ID is required")
        Long slotId,

        @Size(max = 255, message = "Reason cannot exceed 255 characters")
        String reason
) {
}
