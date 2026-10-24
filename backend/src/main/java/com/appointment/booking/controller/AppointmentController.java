package com.appointment.booking.controller;

import com.appointment.booking.dto.request.AppointmentBookingRequest;
import com.appointment.booking.dto.request.AppointmentCancelRequest;
import com.appointment.booking.dto.response.AppointmentResponse;
import com.appointment.booking.model.AppointmentStatus;
import com.appointment.booking.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.appointment.booking.exception.UnauthorizedAppointmentAccessException;
import com.appointment.booking.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> bookAppointment(
            @Valid @RequestBody AppointmentBookingRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Always derive the booking user ID from the verified authenticated principal
        // Prevents callers from forging a different userId
        AppointmentBookingRequest secureRequest = new AppointmentBookingRequest(
                principal.getId(),
                request.slotId(),
                request.reason()
        );

        AppointmentResponse response = appointmentService.bookAppointment(secureRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> getAppointmentById(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        boolean isAdmin = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        AppointmentResponse response = appointmentService.getAppointmentByIdForUser(id, principal.getId(), isAdmin);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AppointmentResponse>> getAppointmentsByUser(
            @PathVariable("userId") Long userId,
            @RequestParam(value = "status", required = false) AppointmentStatus status,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        boolean isAdmin = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !principal.getId().equals(userId)) {
            throw new UnauthorizedAppointmentAccessException(
                    "You are not authorized to view appointments for user id: " + userId
            );
        }

        List<AppointmentResponse> appointments = appointmentService.getAppointmentsByUser(userId, status);
        return ResponseEntity.ok(appointments);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponse> cancelAppointment(
            @PathVariable("id") Long id,
            @Valid @RequestBody(required = false) AppointmentCancelRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Derive caller ID securely from authenticated token
        AppointmentCancelRequest cancelReq = request != null ? request : new AppointmentCancelRequest(null);
        AppointmentResponse response = appointmentService.cancelAppointment(id, principal.getId(), cancelReq);
        return ResponseEntity.ok(response);
    }
}
