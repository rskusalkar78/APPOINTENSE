package com.appointment.booking.repository;

import com.appointment.booking.model.Appointment;
import com.appointment.booking.model.AppointmentSlot;
import com.appointment.booking.model.AppointmentStatus;
import com.appointment.booking.model.Role;
import com.appointment.booking.model.Service;
import com.appointment.booking.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class RepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private AppointmentSlotRepository appointmentSlotRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Test
    @DisplayName("UserRepository: findByEmail and existsByEmail should work correctly")
    void testUserRepository() {
        User user = new User("Alice Green", "alice@example.com", "dummy_hash_123", Role.USER);
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("alice@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("Alice Green");
        assertThat(found.get().getRole()).isEqualTo(Role.USER);

        boolean exists = userRepository.existsByEmail("alice@example.com");
        assertThat(exists).isTrue();

        boolean notExists = userRepository.existsByEmail("nonexistent@example.com");
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("UserRepository: Duplicate email should violate unique constraint")
    void testUserDuplicateEmailConstraint() {
        User user1 = new User("User One", "duplicate@example.com", "hash1", Role.USER);
        userRepository.saveAndFlush(user1);

        User user2 = new User("User Two", "duplicate@example.com", "hash2", Role.USER);
        assertThatThrownBy(() -> userRepository.saveAndFlush(user2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("ServiceRepository: findByActiveTrueOrderByNameAsc should filter inactive services")
    void testServiceRepository() {
        Service s1 = new Service("Consultation", "Doctor consultation", 30, true);
        Service s2 = new Service("Dental Checkup", "Routine checkup", 45, true);
        Service s3 = new Service("Archived Service", "Old service", 15, false);

        serviceRepository.saveAll(List.of(s1, s2, s3));

        List<Service> activeServices = serviceRepository.findByActiveTrueOrderByNameAsc();
        assertThat(activeServices).hasSize(2);
        assertThat(activeServices.get(0).getName()).isEqualTo("Consultation");
        assertThat(activeServices.get(1).getName()).isEqualTo("Dental Checkup");
    }

    @Test
    @DisplayName("AppointmentSlotRepository: Query active slots by service and time window")
    void testAppointmentSlotRepositoryQueries() {
        Service service = serviceRepository.save(new Service("Cardiology", "Heart check", 60, true));

        LocalDateTime base = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        AppointmentSlot slot1 = new AppointmentSlot(service, base, base.plusMinutes(60), true);
        AppointmentSlot slot2 = new AppointmentSlot(service, base.plusHours(2), base.plusHours(3), true);
        AppointmentSlot slot3 = new AppointmentSlot(service, base.plusHours(4), base.plusHours(5), false);

        appointmentSlotRepository.saveAll(List.of(slot1, slot2, slot3));

        List<AppointmentSlot> activeSlots = appointmentSlotRepository
                .findByServiceIdAndActiveTrueAndStartTimeBetweenOrderByStartTimeAsc(
                        service.getId(),
                        base.minusHours(1),
                        base.plusHours(3)
                );

        assertThat(activeSlots).hasSize(2);
    }

    @Test
    @DisplayName("Database Strategy: Prevent duplicate active bookings for the same slot")
    void testPreventDuplicateActiveBookingOnSameSlot() {
        User u1 = userRepository.save(new User("Patient One", "p1@example.com", "h1", Role.USER));
        User u2 = userRepository.save(new User("Patient Two", "p2@example.com", "h2", Role.USER));
        Service service = serviceRepository.save(new Service("General Checkup", "Clinic checkup", 30, true));

        LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);
        AppointmentSlot slot = appointmentSlotRepository.save(
                new AppointmentSlot(service, start, start.plusMinutes(30), true)
        );

        Appointment firstBooking = new Appointment(u1, slot, AppointmentStatus.BOOKED, "First patient routine");
        appointmentRepository.saveAndFlush(firstBooking);

        // Attempt second concurrent or duplicate booking for the exact same slot while first is BOOKED
        Appointment duplicateBooking = new Appointment(u2, slot, AppointmentStatus.BOOKED, "Second patient competing");

        assertThatThrownBy(() -> appointmentRepository.saveAndFlush(duplicateBooking))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Database Strategy: Cancelled appointment frees slot token for future booking")
    void testCancelledBookingAllowsRebooking() {
        User u1 = userRepository.save(new User("Patient Alpha", "alpha@example.com", "h1", Role.USER));
        User u2 = userRepository.save(new User("Patient Beta", "beta@example.com", "h2", Role.USER));
        Service service = serviceRepository.save(new Service("Therapy", "Physical therapy", 45, true));

        LocalDateTime start = LocalDateTime.now().plusDays(3).withHour(14).withMinute(0);
        AppointmentSlot slot = appointmentSlotRepository.save(
                new AppointmentSlot(service, start, start.plusMinutes(45), true)
        );

        Appointment booking1 = new Appointment(u1, slot, AppointmentStatus.BOOKED, "Initial booking");
        appointmentRepository.saveAndFlush(booking1);

        // User 1 cancels appointment
        booking1.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.saveAndFlush(booking1);

        // User 2 now books the slot - should succeed without constraint violation
        Appointment booking2 = new Appointment(u2, slot, AppointmentStatus.BOOKED, "Re-booked by patient beta");
        Appointment savedBooking2 = appointmentRepository.saveAndFlush(booking2);

        assertThat(savedBooking2.getId()).isNotNull();
        assertThat(savedBooking2.getStatus()).isEqualTo(AppointmentStatus.BOOKED);
    }

    @Test
    @DisplayName("AppointmentRepository: findByUserIdAndStatusOrderByCreatedAtDesc")
    void testFindByUserIdAndStatus() {
        User user = userRepository.save(new User("John Doe", "john@example.com", "h3", Role.USER));
        Service service = serviceRepository.save(new Service("Eye Exam", "Vision check", 20, true));

        LocalDateTime time1 = LocalDateTime.now().plusDays(1).withHour(11).withMinute(0);
        LocalDateTime time2 = LocalDateTime.now().plusDays(2).withHour(11).withMinute(0);

        AppointmentSlot slot1 = appointmentSlotRepository.save(new AppointmentSlot(service, time1, time1.plusMinutes(20), true));
        AppointmentSlot slot2 = appointmentSlotRepository.save(new AppointmentSlot(service, time2, time2.plusMinutes(20), true));

        Appointment a1 = new Appointment(user, slot1, AppointmentStatus.BOOKED, "Regular checkup");
        Appointment a2 = new Appointment(user, slot2, AppointmentStatus.CANCELLED, "Had conflict");

        appointmentRepository.saveAllAndFlush(List.of(a1, a2));

        List<Appointment> bookedList = appointmentRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), AppointmentStatus.BOOKED);
        assertThat(bookedList).hasSize(1);
        assertThat(bookedList.get(0).getSlot().getId()).isEqualTo(slot1.getId());

        List<Appointment> cancelledList = appointmentRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), AppointmentStatus.CANCELLED);
        assertThat(cancelledList).hasSize(1);
        assertThat(cancelledList.get(0).getSlot().getId()).isEqualTo(slot2.getId());
    }
}
