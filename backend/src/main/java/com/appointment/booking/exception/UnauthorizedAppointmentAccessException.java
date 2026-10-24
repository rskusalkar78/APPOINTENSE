package com.appointment.booking.exception;

public class UnauthorizedAppointmentAccessException extends RuntimeException {
    public UnauthorizedAppointmentAccessException(String message) {
        super(message);
    }
}
