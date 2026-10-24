package com.appointment.booking.controller;

import com.appointment.booking.dto.request.ServiceCreateRequest;
import com.appointment.booking.dto.response.ServiceResponse;
import com.appointment.booking.service.ServiceManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceCatalogController {

    private final ServiceManagementService serviceManagementService;

    public ServiceCatalogController(ServiceManagementService serviceManagementService) {
        this.serviceManagementService = serviceManagementService;
    }

    @PostMapping
    public ResponseEntity<ServiceResponse> createService(@Valid @RequestBody ServiceCreateRequest request) {
        ServiceResponse response = serviceManagementService.createService(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ServiceResponse>> listActiveServices() {
        List<ServiceResponse> services = serviceManagementService.getActiveServices();
        return ResponseEntity.ok(services);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceResponse> getServiceById(@PathVariable("id") Long id) {
        ServiceResponse service = serviceManagementService.getServiceById(id);
        return ResponseEntity.ok(service);
    }
}
