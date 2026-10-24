package com.appointment.booking.exception;

public class InvalidAppointmentOperationException extends RuntimeException {
    public InvalidAppointmentOperationException(String message) {
        super(message);
    }
}
