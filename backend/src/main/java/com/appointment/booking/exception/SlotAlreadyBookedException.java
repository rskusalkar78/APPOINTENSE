package com.appointment.booking.exception;

public class SlotAlreadyBookedException extends RuntimeException {
    public SlotAlreadyBookedException(String message) {
        super(message);
    }

    public SlotAlreadyBookedException(String message, Throwable cause) {
        super(message, cause);
    }
}
