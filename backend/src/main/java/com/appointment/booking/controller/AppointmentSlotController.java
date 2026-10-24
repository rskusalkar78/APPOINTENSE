package com.appointment.booking.controller;

import com.appointment.booking.dto.request.SlotCreateRequest;
import com.appointment.booking.dto.response.SlotResponse;
import com.appointment.booking.exception.InvalidAppointmentOperationException;
import com.appointment.booking.service.AppointmentSlotService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/slots")
public class AppointmentSlotController {

    private final AppointmentSlotService slotService;

    public AppointmentSlotController(AppointmentSlotService slotService) {
        this.slotService = slotService;
    }

    @PostMapping
    public ResponseEntity<SlotResponse> createSlot(@Valid @RequestBody SlotCreateRequest request) {
        SlotResponse response = slotService.createSlot(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SlotResponse>> listBookableSlots(
            @RequestParam(value = "serviceId", required = false) Long serviceId,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new InvalidAppointmentOperationException("'to' time cannot be earlier than 'from' time");
        }

        List<SlotResponse> slots = slotService.getBookableSlots(serviceId, from, to);
        return ResponseEntity.ok(slots);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SlotResponse> getSlotById(@PathVariable("id") Long id) {
        SlotResponse slot = slotService.getSlotById(id);
        return ResponseEntity.ok(slot);
    }
}
