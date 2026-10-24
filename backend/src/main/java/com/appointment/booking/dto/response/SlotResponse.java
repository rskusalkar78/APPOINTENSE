package com.appointment.booking.dto.response;

import com.appointment.booking.model.AppointmentSlot;

import java.time.LocalDateTime;

public record SlotResponse(
        Long id,
        Long serviceId,
        String serviceName,
        Integer serviceDurationMinutes,
        LocalDateTime startTime,
        LocalDateTime endTime,
        boolean active,
        LocalDateTime createdAt
) {
    public static SlotResponse fromEntity(AppointmentSlot slot) {
        if (slot == null) {
            return null;
        }
        Long serviceId = slot.getService() != null ? slot.getService().getId() : null;
        String serviceName = slot.getService() != null ? slot.getService().getName() : null;
        Integer serviceDuration = slot.getService() != null ? slot.getService().getDurationMinutes() : null;

        return new SlotResponse(
                slot.getId(),
                serviceId,
                serviceName,
                serviceDuration,
                slot.getStartTime(),
                slot.getEndTime(),
                slot.isActive(),
                slot.getCreatedAt()
        );
    }
}
