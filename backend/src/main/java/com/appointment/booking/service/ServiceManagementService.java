package com.appointment.booking.service;

import com.appointment.booking.dto.request.ServiceCreateRequest;
import com.appointment.booking.dto.response.ServiceResponse;

import java.util.List;

public interface ServiceManagementService {

    ServiceResponse createService(ServiceCreateRequest request);

    List<ServiceResponse> getActiveServices();

    ServiceResponse getServiceById(Long serviceId);
}
