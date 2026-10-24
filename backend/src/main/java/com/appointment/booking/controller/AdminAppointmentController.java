package com.appointment.booking.controller;

import com.appointment.booking.dto.response.AppointmentResponse;
import com.appointment.booking.model.AppointmentStatus;
import com.appointment.booking.service.AppointmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/appointments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAppointmentController {

    private final AppointmentService appointmentService;

    public AdminAppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public ResponseEntity<List<AppointmentResponse>> listAllAppointments(
            @RequestParam(value = "status", required = false) AppointmentStatus status
    ) {
        List<AppointmentResponse> list = appointmentService.getAllAppointments(status);
        return ResponseEntity.ok(list);
    }
}
