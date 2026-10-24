package com.appointment.booking.service.impl;

import com.appointment.booking.dto.request.ServiceCreateRequest;
import com.appointment.booking.dto.response.ServiceResponse;
import com.appointment.booking.exception.DuplicateResourceException;
import com.appointment.booking.exception.ResourceNotFoundException;
import com.appointment.booking.model.Service;
import com.appointment.booking.repository.ServiceRepository;
import com.appointment.booking.service.ServiceManagementService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@org.springframework.stereotype.Service
@Transactional(readOnly = true)
public class ServiceManagementServiceImpl implements ServiceManagementService {

    private final ServiceRepository serviceRepository;

    public ServiceManagementServiceImpl(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @Override
    @Transactional
    public ServiceResponse createService(ServiceCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Service request cannot be null");
        }

        String trimmedName = request.name().trim();
        if (serviceRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new DuplicateResourceException("Service with name '" + trimmedName + "' already exists");
        }

        Service service = new Service();
        service.setName(trimmedName);
        service.setDescription(request.description() != null ? request.description().trim() : null);
        service.setDurationMinutes(request.durationMinutes());
        service.setActive(request.active() == null || request.active());

        Service savedService = serviceRepository.save(service);
        return ServiceResponse.fromEntity(savedService);
    }

    @Override
    public List<ServiceResponse> getActiveServices() {
        return serviceRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(ServiceResponse::fromEntity)
                .toList();
    }

    @Override
    public ServiceResponse getServiceById(Long serviceId) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + serviceId));
        return ServiceResponse.fromEntity(service);
    }
}
