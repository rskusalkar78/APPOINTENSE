package com.appointment.booking.service;

import com.appointment.booking.dto.request.AppointmentBookingRequest;
import com.appointment.booking.dto.request.AppointmentCancelRequest;
import com.appointment.booking.dto.response.AppointmentResponse;
import com.appointment.booking.exception.SlotAlreadyBookedException;
import com.appointment.booking.model.AppointmentSlot;
import com.appointment.booking.model.AppointmentStatus;
import com.appointment.booking.model.Role;
import com.appointment.booking.model.Service;
import com.appointment.booking.model.User;
import com.appointment.booking.repository.AppointmentRepository;
import com.appointment.booking.repository.AppointmentSlotRepository;
import com.appointment.booking.repository.ServiceRepository;
import com.appointment.booking.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AppointmentServiceConcurrencyIntegrationTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private AppointmentSlotRepository appointmentSlotRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Test
    @DisplayName("Concurrency: Simultaneous booking attempts for the same slot allow exactly one winner")
    void testSimultaneousBookingAttemptsForSameSlot() throws InterruptedException {
        // 1. Setup service and single appointment slot
        Service service = serviceRepository.save(new Service("Neurology", "Brain check", 60, true));
        LocalDateTime start = LocalDateTime.now().plusDays(5).withHour(10).withMinute(0);
        AppointmentSlot slot = appointmentSlotRepository.save(new AppointmentSlot(service, start, start.plusMinutes(60), true));

        // 2. Setup 10 distinct users competing for this single slot
        int threadCount = 10;
        List<User> users = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            users.add(userRepository.save(new User("Applicant " + i, "applicant" + i + "@example.com", null, Role.USER)));
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger alreadyBookedCounter = new AtomicInteger(0);
        List<Throwable> unexpectedErrors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final User applicant = users.get(i);
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startGate.await(); // ensure all threads fire at the exact same millisecond
                    appointmentService.bookAppointment(new AppointmentBookingRequest(applicant.getId(), slot.getId(), "Competing for slot"));
                    successCounter.incrementAndGet();
                } catch (SlotAlreadyBookedException sabe) {
                    alreadyBookedCounter.incrementAndGet();
                } catch (Throwable t) {
                    unexpectedErrors.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startGate.countDown(); // release all threads simultaneously
        doneLatch.await();
        executor.shutdown();

        // 3. Verify invariants: Exactly ONE booking succeeded, others caught SlotAlreadyBookedException
        assertThat(unexpectedErrors).isEmpty();
        assertThat(successCounter.get()).isEqualTo(1);
        assertThat(alreadyBookedCounter.get()).isEqualTo(threadCount - 1);

        // 4. Verify database state
        List<com.appointment.booking.model.Appointment> savedBookings = appointmentRepository.findByStatusOrderByCreatedAtDesc(AppointmentStatus.BOOKED)
                .stream()
                .filter(a -> a.getSlot().getId().equals(slot.getId()))
                .toList();

        assertThat(savedBookings).hasSize(1);
        assertThat(savedBookings.get(0).getActiveSlotToken()).isEqualTo("SLOT_" + slot.getId());
    }

    @Test
    @DisplayName("Lifecycle: Cancellation frees slot and allows subsequent rebooking while retaining history")
    void testCancelAndRebookIntegration() {
        Service service = serviceRepository.save(new Service("Dermatology", "Skin care", 30, true));
        LocalDateTime start = LocalDateTime.now().plusDays(6).withHour(11).withMinute(0);
        AppointmentSlot slot = appointmentSlotRepository.save(new AppointmentSlot(service, start, start.plusMinutes(30), true));

        User user1 = userRepository.save(new User("First User", "first.user@example.com", null, Role.USER));
        User user2 = userRepository.save(new User("Second User", "second.user@example.com", null, Role.USER));

        // User 1 books
        AppointmentResponse b1 = appointmentService.bookAppointment(new AppointmentBookingRequest(user1.getId(), slot.getId(), "Acne issue"));
        assertThat(b1.status()).isEqualTo(AppointmentStatus.BOOKED);

        // User 1 cancels
        AppointmentResponse c1 = appointmentService.cancelAppointment(b1.id(), user1.getId(), new AppointmentCancelRequest("Change of plans"));
        assertThat(c1.status()).isEqualTo(AppointmentStatus.CANCELLED);

        // User 2 now books the same slot
        AppointmentResponse b2 = appointmentService.bookAppointment(new AppointmentBookingRequest(user2.getId(), slot.getId(), "Consultation"));
        assertThat(b2.status()).isEqualTo(AppointmentStatus.BOOKED);
        assertThat(b2.userId()).isEqualTo(user2.getId());

        // Both appointment records exist in history
        List<AppointmentResponse> historyUser1 = appointmentService.getAppointmentsByUser(user1.getId(), null);
        List<AppointmentResponse> historyUser2 = appointmentService.getAppointmentsByUser(user2.getId(), null);

        assertThat(historyUser1).hasSize(1);
        assertThat(historyUser1.get(0).status()).isEqualTo(AppointmentStatus.CANCELLED);

        assertThat(historyUser2).hasSize(1);
        assertThat(historyUser2.get(0).status()).isEqualTo(AppointmentStatus.BOOKED);
    }
}
