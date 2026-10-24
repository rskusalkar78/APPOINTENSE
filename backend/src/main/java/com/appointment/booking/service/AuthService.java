package com.appointment.booking.service;

import com.appointment.booking.dto.request.UserLoginRequest;
import com.appointment.booking.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse login(UserLoginRequest request);
}
