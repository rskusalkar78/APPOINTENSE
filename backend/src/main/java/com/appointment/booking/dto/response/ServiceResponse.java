package com.appointment.booking.dto.response;

import com.appointment.booking.model.Service;

import java.time.LocalDateTime;

public record ServiceResponse(
        Long id,
        String name,
        String description,
        Integer durationMinutes,
        boolean active,
        LocalDateTime createdAt
) {
    public static ServiceResponse fromEntity(Service service) {
        if (service == null) {
            return null;
        }
        return new ServiceResponse(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.getDurationMinutes(),
                service.isActive(),
                service.getCreatedAt()
        );
    }
}
