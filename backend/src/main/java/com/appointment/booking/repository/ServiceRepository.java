package com.appointment.booking.repository;

import com.appointment.booking.model.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {

    List<Service> findByActiveTrueOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
