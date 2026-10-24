package com.appointment.booking.repository;

import com.appointment.booking.model.Appointment;
import com.appointment.booking.model.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Appointment> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, AppointmentStatus status);

    List<Appointment> findByStatusOrderByCreatedAtDesc(AppointmentStatus status);

    Optional<Appointment> findBySlotIdAndStatus(Long slotId, AppointmentStatus status);

    boolean existsBySlotIdAndStatus(Long slotId, AppointmentStatus status);

    @Query("""
            SELECT a FROM Appointment a
            JOIN FETCH a.user u
            JOIN FETCH a.slot s
            JOIN FETCH s.service
            WHERE a.id = :id
            """)
    Optional<Appointment> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT a FROM Appointment a
            JOIN FETCH a.slot s
            JOIN FETCH s.service
            WHERE a.user.id = :userId
            ORDER BY s.startTime DESC
            """)
    List<Appointment> findAppointmentsByUserWithSlotDetails(@Param("userId") Long userId);

    @Query("""
            SELECT a FROM Appointment a
            JOIN FETCH a.user
            JOIN FETCH a.slot s
            JOIN FETCH s.service
            WHERE s.startTime BETWEEN :start AND :end
            ORDER BY s.startTime ASC
            """)
    List<Appointment> findAppointmentsInTimeRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
