package com.appointment.booking.service;

import com.appointment.booking.dto.request.AppointmentBookingRequest;
import com.appointment.booking.dto.request.AppointmentCancelRequest;
import com.appointment.booking.dto.response.AppointmentResponse;
import com.appointment.booking.model.AppointmentStatus;

import java.util.List;

public interface AppointmentService {

    AppointmentResponse bookAppointment(AppointmentBookingRequest request);

    AppointmentResponse cancelAppointment(Long appointmentId, Long requestingUserId, AppointmentCancelRequest request);

    AppointmentResponse getAppointmentById(Long appointmentId);

    AppointmentResponse getAppointmentByIdForUser(Long appointmentId, Long requestingUserId, boolean isAdmin);

    List<AppointmentResponse> getAppointmentsByUser(Long userId, AppointmentStatus status);

    List<AppointmentResponse> getAllAppointments(AppointmentStatus status);
}
