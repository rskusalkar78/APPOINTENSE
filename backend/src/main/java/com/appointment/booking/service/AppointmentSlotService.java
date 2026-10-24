package com.appointment.booking.service;

import com.appointment.booking.dto.request.SlotCreateRequest;
import com.appointment.booking.dto.response.SlotResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentSlotService {

    SlotResponse createSlot(SlotCreateRequest request);

    List<SlotResponse> getBookableSlots(Long serviceId, LocalDateTime start, LocalDateTime end);

    SlotResponse getSlotById(Long slotId);
}
