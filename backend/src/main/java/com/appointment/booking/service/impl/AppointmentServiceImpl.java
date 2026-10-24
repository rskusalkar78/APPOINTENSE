package com.appointment.booking.service.impl;

import com.appointment.booking.dto.request.AppointmentBookingRequest;
import com.appointment.booking.dto.request.AppointmentCancelRequest;
import com.appointment.booking.dto.response.AppointmentResponse;
import com.appointment.booking.exception.InvalidAppointmentOperationException;
import com.appointment.booking.exception.ResourceNotFoundException;
import com.appointment.booking.exception.SlotAlreadyBookedException;
import com.appointment.booking.exception.UnauthorizedAppointmentAccessException;
import com.appointment.booking.model.Appointment;
import com.appointment.booking.model.AppointmentSlot;
import com.appointment.booking.model.AppointmentStatus;
import com.appointment.booking.model.User;
import com.appointment.booking.repository.AppointmentRepository;
import com.appointment.booking.repository.AppointmentSlotRepository;
import com.appointment.booking.repository.UserRepository;
import com.appointment.booking.service.AppointmentService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentSlotRepository slotRepository;
    private final UserRepository userRepository;

    public AppointmentServiceImpl(
            AppointmentRepository appointmentRepository,
            AppointmentSlotRepository slotRepository,
            UserRepository userRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.slotRepository = slotRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public AppointmentResponse bookAppointment(AppointmentBookingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Booking request cannot be null");
        }

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.userId()));

        AppointmentSlot slot = slotRepository.findById(request.slotId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment slot not found with id: " + request.slotId()));

        if (!slot.isActive()) {
            throw new InvalidAppointmentOperationException("Cannot book an inactive appointment slot");
        }

        if (slot.getService() != null && !slot.getService().isActive()) {
            throw new InvalidAppointmentOperationException("Cannot book a slot for an inactive service");
        }

        if (slot.getStartTime().isBefore(LocalDateTime.now())) {
            throw new InvalidAppointmentOperationException("Cannot book a slot in the past");
        }

        // Fast-path application check (optimization, not sole concurrency guarantee)
        if (appointmentRepository.existsBySlotIdAndStatus(slot.getId(), AppointmentStatus.BOOKED)) {
            throw new SlotAlreadyBookedException("Slot id " + slot.getId() + " is already booked");
        }

        Appointment appointment = new Appointment();
        appointment.setUser(user);
        appointment.setSlot(slot);
        appointment.setStatus(AppointmentStatus.BOOKED);
        appointment.setReason(request.reason() != null ? request.reason().trim() : null);
        // Explicitly set deterministic concurrency token: "SLOT_" + slotId
        appointment.syncActiveSlotToken();

        try {
            // saveAndFlush forces SQL insert within the current transaction boundary
            // so database unique constraint violations trigger immediately and can be caught & translated
            Appointment savedAppointment = appointmentRepository.saveAndFlush(appointment);
            return AppointmentResponse.fromEntity(savedAppointment);
        } catch (DataIntegrityViolationException dive) {
            // Concurrency defense: If two threads booked the slot at the exact same millisecond,
            // MySQL unique constraint uk_active_slot_booking_token rejects the duplicate insert.
            throw new SlotAlreadyBookedException(
                    "Slot id " + slot.getId() + " has already been booked by another request",
                    dive
            );
        }
    }

    @Override
    @Transactional
    public AppointmentResponse cancelAppointment(Long appointmentId, Long requestingUserId, AppointmentCancelRequest request) {
        if (appointmentId == null) {
            throw new IllegalArgumentException("Appointment id is required");
        }
        if (requestingUserId == null) {
            throw new IllegalArgumentException("Requesting user id is required");
        }

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));

        // Authorization check: Only the owner of the appointment can cancel it
        if (!appointment.getUser().getId().equals(requestingUserId)) {
            throw new UnauthorizedAppointmentAccessException(
                    "User id " + requestingUserId + " is not authorized to cancel appointment id " + appointmentId
            );
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new InvalidAppointmentOperationException("Appointment is already cancelled");
        }

        // Update status and clear the active_slot_token so the slot can be re-booked if needed
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.syncActiveSlotToken(); // sets activeSlotToken = null

        if (request != null && request.cancellationReason() != null && !request.cancellationReason().isBlank()) {
            String combinedReason = appointment.getReason() != null
                    ? appointment.getReason() + " [Cancelled: " + request.cancellationReason().trim() + "]"
                    : "[Cancelled: " + request.cancellationReason().trim() + "]";
            if (combinedReason.length() > 255) {
                combinedReason = combinedReason.substring(0, 255);
            }
            appointment.setReason(combinedReason);
        }

        Appointment updated = appointmentRepository.saveAndFlush(appointment);
        return AppointmentResponse.fromEntity(updated);
    }

    @Override
    public AppointmentResponse getAppointmentById(Long appointmentId) {
        Appointment appointment = appointmentRepository.findByIdWithDetails(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));
        return AppointmentResponse.fromEntity(appointment);
    }

    @Override
    public AppointmentResponse getAppointmentByIdForUser(Long appointmentId, Long requestingUserId, boolean isAdmin) {
        Appointment appointment = appointmentRepository.findByIdWithDetails(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));

        if (!isAdmin && !appointment.getUser().getId().equals(requestingUserId)) {
            throw new UnauthorizedAppointmentAccessException(
                    "User id " + requestingUserId + " is not authorized to view appointment id " + appointmentId
            );
        }

        return AppointmentResponse.fromEntity(appointment);
    }

    @Override
    public List<AppointmentResponse> getAppointmentsByUser(Long userId, AppointmentStatus status) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        List<Appointment> appointments;
        if (status != null) {
            appointments = appointmentRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
        } else {
            appointments = appointmentRepository.findAppointmentsByUserWithSlotDetails(userId);
        }

        return appointments.stream()
                .map(AppointmentResponse::fromEntity)
                .toList();
    }

    @Override
    public List<AppointmentResponse> getAllAppointments(AppointmentStatus status) {
        List<Appointment> appointments;
        if (status != null) {
            appointments = appointmentRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            appointments = appointmentRepository.findAll();
        }
        return appointments.stream()
                .map(AppointmentResponse::fromEntity)
                .toList();
    }
}
