package com.appointment.booking.service.impl;

import com.appointment.booking.dto.request.SlotCreateRequest;
import com.appointment.booking.dto.response.SlotResponse;
import com.appointment.booking.exception.DuplicateResourceException;
import com.appointment.booking.exception.InvalidAppointmentOperationException;
import com.appointment.booking.exception.ResourceNotFoundException;
import com.appointment.booking.model.AppointmentSlot;
import com.appointment.booking.model.AppointmentStatus;
import com.appointment.booking.model.Service;
import com.appointment.booking.repository.AppointmentRepository;
import com.appointment.booking.repository.AppointmentSlotRepository;
import com.appointment.booking.repository.ServiceRepository;
import com.appointment.booking.service.AppointmentSlotService;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@org.springframework.stereotype.Service
@Transactional(readOnly = true)
public class AppointmentSlotServiceImpl implements AppointmentSlotService {

    private final AppointmentSlotRepository slotRepository;
    private final ServiceRepository serviceRepository;
    private final AppointmentRepository appointmentRepository;

    public AppointmentSlotServiceImpl(
            AppointmentSlotRepository slotRepository,
            ServiceRepository serviceRepository,
            AppointmentRepository appointmentRepository
    ) {
        this.slotRepository = slotRepository;
        this.serviceRepository = serviceRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    @Transactional
    public SlotResponse createSlot(SlotCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Slot creation request cannot be null");
        }

        if (request.startTime() == null || request.endTime() == null) {
            throw new InvalidAppointmentOperationException("Start time and end time are required");
        }

        if (!request.endTime().isAfter(request.startTime())) {
            throw new InvalidAppointmentOperationException("Slot end time must be strictly after start time");
        }

        Service service = serviceRepository.findById(request.serviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + request.serviceId()));

        if (!service.isActive()) {
            throw new InvalidAppointmentOperationException("Cannot create a slot for an inactive service");
        }

        if (slotRepository.existsByServiceIdAndStartTime(service.getId(), request.startTime())) {
            throw new DuplicateResourceException(
                    "A slot for service '" + service.getName() + "' at start time " + request.startTime() + " already exists"
            );
        }

        AppointmentSlot slot = new AppointmentSlot();
        slot.setService(service);
        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());
        slot.setActive(request.active() == null || request.active());

        AppointmentSlot savedSlot = slotRepository.save(slot);
        return SlotResponse.fromEntity(savedSlot);
    }

    @Override
    public List<SlotResponse> getBookableSlots(Long serviceId, LocalDateTime start, LocalDateTime end) {
        List<AppointmentSlot> activeSlots;

        LocalDateTime queryStart = start != null ? start : LocalDateTime.now();
        LocalDateTime queryEnd = end != null ? end : queryStart.plusMonths(1);

        if (serviceId != null) {
            if (!serviceRepository.existsById(serviceId)) {
                throw new ResourceNotFoundException("Service not found with id: " + serviceId);
            }
            activeSlots = slotRepository.findByServiceIdAndActiveTrueAndStartTimeBetweenOrderByStartTimeAsc(
                    serviceId,
                    queryStart,
                    queryEnd
            );
        } else {
            activeSlots = slotRepository.findByActiveTrueAndStartTimeBetweenOrderByStartTimeAsc(
                    queryStart,
                    queryEnd
            );
        }

        // Filter out any slot that currently has an active BOOKED appointment
        return activeSlots.stream()
                .filter(slot -> !appointmentRepository.existsBySlotIdAndStatus(slot.getId(), AppointmentStatus.BOOKED))
                .map(SlotResponse::fromEntity)
                .toList();
    }

    @Override
    public SlotResponse getSlotById(Long slotId) {
        AppointmentSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment slot not found with id: " + slotId));
        return SlotResponse.fromEntity(slot);
    }
}
