package com.appointment.booking.repository;

import com.appointment.booking.model.AppointmentSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentSlotRepository extends JpaRepository<AppointmentSlot, Long> {

    List<AppointmentSlot> findByServiceIdAndActiveTrueAndStartTimeBetweenOrderByStartTimeAsc(
            Long serviceId,
            LocalDateTime start,
            LocalDateTime end
    );

    List<AppointmentSlot> findByActiveTrueAndStartTimeBetweenOrderByStartTimeAsc(
            LocalDateTime start,
            LocalDateTime end
    );

    List<AppointmentSlot> findByServiceIdOrderByStartTimeAsc(Long serviceId);

    @Query("""
            SELECT s FROM AppointmentSlot s
            WHERE s.service.id = :serviceId
              AND s.active = true
              AND s.startTime >= :fromTime
              AND s.id NOT IN (
                  SELECT a.slot.id FROM Appointment a
                  WHERE a.status = com.appointment.booking.model.AppointmentStatus.BOOKED
              )
            ORDER BY s.startTime ASC
            """)
    List<AppointmentSlot> findAvailableSlotsByServiceAfter(
            @Param("serviceId") Long serviceId,
            @Param("fromTime") LocalDateTime fromTime
    );

    boolean existsByServiceIdAndStartTime(Long serviceId, LocalDateTime startTime);
}
