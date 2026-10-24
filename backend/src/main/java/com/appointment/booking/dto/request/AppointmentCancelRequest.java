package com.appointment.booking.dto.request;

import jakarta.validation.constraints.Size;

public record AppointmentCancelRequest(
        @Size(max = 255, message = "Cancellation reason cannot exceed 255 characters")
        String cancellationReason
) {
}
