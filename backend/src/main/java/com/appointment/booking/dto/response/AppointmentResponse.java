package com.appointment.booking.dto.response;

import com.appointment.booking.model.Appointment;
import com.appointment.booking.model.AppointmentStatus;

import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,
        Long userId,
        String userFullName,
        String userEmail,
        Long slotId,
        Long serviceId,
        String serviceName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        AppointmentStatus status,
        String reason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AppointmentResponse fromEntity(Appointment appointment) {
        if (appointment == null) {
            return null;
        }

        Long userId = appointment.getUser() != null ? appointment.getUser().getId() : null;
        String userFullName = appointment.getUser() != null ? appointment.getUser().getFullName() : null;
        String userEmail = appointment.getUser() != null ? appointment.getUser().getEmail() : null;

        Long slotId = appointment.getSlot() != null ? appointment.getSlot().getId() : null;
        Long serviceId = null;
        String serviceName = null;
        LocalDateTime startTime = null;
        LocalDateTime endTime = null;

        if (appointment.getSlot() != null) {
            startTime = appointment.getSlot().getStartTime();
            endTime = appointment.getSlot().getEndTime();
            if (appointment.getSlot().getService() != null) {
                serviceId = appointment.getSlot().getService().getId();
                serviceName = appointment.getSlot().getService().getName();
            }
        }

        return new AppointmentResponse(
                appointment.getId(),
                userId,
                userFullName,
                userEmail,
                slotId,
                serviceId,
                serviceName,
                startTime,
                endTime,
                appointment.getStatus(),
                appointment.getReason(),
                appointment.getCreatedAt(),
                appointment.getUpdatedAt()
        );
    }
}
