package com.appointment.booking.service;

import com.appointment.booking.dto.request.UserRegistrationRequest;
import com.appointment.booking.dto.response.UserResponse;

public interface UserService {

    UserResponse registerUser(UserRegistrationRequest request);

    UserResponse getUserById(Long userId);

    UserResponse getUserByEmail(String email);
}
