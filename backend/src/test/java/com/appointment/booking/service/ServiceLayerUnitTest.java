package com.appointment.booking.service;

import com.appointment.booking.dto.request.AppointmentBookingRequest;
import com.appointment.booking.dto.request.AppointmentCancelRequest;
import com.appointment.booking.dto.request.ServiceCreateRequest;
import com.appointment.booking.dto.request.SlotCreateRequest;
import com.appointment.booking.dto.request.UserRegistrationRequest;
import com.appointment.booking.dto.response.AppointmentResponse;
import com.appointment.booking.dto.response.ServiceResponse;
import com.appointment.booking.dto.response.SlotResponse;
import com.appointment.booking.dto.response.UserResponse;
import com.appointment.booking.exception.DuplicateResourceException;
import com.appointment.booking.exception.InvalidAppointmentOperationException;
import com.appointment.booking.exception.ResourceNotFoundException;
import com.appointment.booking.exception.SlotAlreadyBookedException;
import com.appointment.booking.exception.UnauthorizedAppointmentAccessException;
import com.appointment.booking.model.Appointment;
import com.appointment.booking.model.AppointmentSlot;
import com.appointment.booking.model.AppointmentStatus;
import com.appointment.booking.model.Role;
import com.appointment.booking.model.Service;
import com.appointment.booking.model.User;
import com.appointment.booking.repository.AppointmentRepository;
import com.appointment.booking.repository.AppointmentSlotRepository;
import com.appointment.booking.repository.ServiceRepository;
import com.appointment.booking.repository.UserRepository;
import com.appointment.booking.service.impl.AppointmentServiceImpl;
import com.appointment.booking.service.impl.AppointmentSlotServiceImpl;
import com.appointment.booking.service.impl.ServiceManagementServiceImpl;
import com.appointment.booking.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceLayerUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private AppointmentSlotRepository slotRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private UserService userService;
    private ServiceManagementService serviceManagementService;
    private AppointmentSlotService appointmentSlotService;
    private AppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, passwordEncoder);
        serviceManagementService = new ServiceManagementServiceImpl(serviceRepository);
        appointmentSlotService = new AppointmentSlotServiceImpl(slotRepository, serviceRepository, appointmentRepository);
        appointmentService = new AppointmentServiceImpl(appointmentRepository, slotRepository, userRepository);
    }

    @Test
    @DisplayName("UserService: Successful user registration normalizes email and preserves role")
    void testRegisterUserSuccess() {
        UserRegistrationRequest req = new UserRegistrationRequest("Bob Martin", "  Bob@Example.COM  ", "secret1234");
        when(userRepository.existsByEmail("bob@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_secret");

        User savedUser = new User("Bob Martin", "bob@example.com", "hashed_secret", Role.USER);
        savedUser.setId(10L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = userService.registerUser(req);
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.email()).isEqualTo("bob@example.com");
        assertThat(response.fullName()).isEqualTo("Bob Martin");
        assertThat(response.role()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("UserService: Duplicate email throws DuplicateResourceException")
    void testRegisterUserDuplicateEmail() {
        UserRegistrationRequest req = new UserRegistrationRequest("Bob Martin", "bob@example.com", "secret123");
        when(userRepository.existsByEmail("bob@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("ServiceManagementService: Create service successfully and reject duplicate name")
    void testServiceCreationAndDuplicateRejection() {
        ServiceCreateRequest req = new ServiceCreateRequest("Dentistry", "Dental care", 30, true);
        when(serviceRepository.existsByNameIgnoreCase("Dentistry")).thenReturn(false);

        Service saved = new Service("Dentistry", "Dental care", 30, true);
        saved.setId(1L);
        when(serviceRepository.save(any(Service.class))).thenReturn(saved);

        ServiceResponse res = serviceManagementService.createService(req);
        assertThat(res.id()).isEqualTo(1L);
        assertThat(res.name()).isEqualTo("Dentistry");

        when(serviceRepository.existsByNameIgnoreCase("Dentistry")).thenReturn(true);
        assertThatThrownBy(() -> serviceManagementService.createService(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("AppointmentSlotService: Rejects invalid slot times where end <= start")
    void testSlotInvalidTimes() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime invalidEnd = start.minusHours(1);

        SlotCreateRequest req = new SlotCreateRequest(1L, start, invalidEnd, true);
        assertThatThrownBy(() -> appointmentSlotService.createSlot(req))
                .isInstanceOf(InvalidAppointmentOperationException.class)
                .hasMessageContaining("strictly after start time");
    }

    @Test
    @DisplayName("AppointmentSlotService: Rejects duplicate slot for same service and start time")
    void testSlotDuplicate() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime end = start.plusMinutes(30);

        Service service = new Service("General", "Care", 30, true);
        service.setId(1L);

        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));
        when(slotRepository.existsByServiceIdAndStartTime(1L, start)).thenReturn(true);

        SlotCreateRequest req = new SlotCreateRequest(1L, start, end, true);
        assertThatThrownBy(() -> appointmentSlotService.createSlot(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("AppointmentService: Successful appointment booking")
    void testBookAppointmentSuccess() {
        User user = new User("Alice", "alice@example.com", null, Role.USER);
        user.setId(5L);

        Service service = new Service("Physiotherapy", "Rehab", 45, true);
        service.setId(1L);

        LocalDateTime start = LocalDateTime.now().plusDays(2);
        AppointmentSlot slot = new AppointmentSlot(service, start, start.plusMinutes(45), true);
        slot.setId(20L);

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(slotRepository.findById(20L)).thenReturn(Optional.of(slot));
        when(appointmentRepository.existsBySlotIdAndStatus(20L, AppointmentStatus.BOOKED)).thenReturn(false);

        Appointment savedAppointment = new Appointment(user, slot, AppointmentStatus.BOOKED, "Knee injury");
        savedAppointment.setId(100L);
        when(appointmentRepository.saveAndFlush(any(Appointment.class))).thenReturn(savedAppointment);

        AppointmentBookingRequest request = new AppointmentBookingRequest(5L, 20L, "Knee injury");
        AppointmentResponse response = appointmentService.bookAppointment(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo(AppointmentStatus.BOOKED);
        assertThat(response.slotId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("AppointmentService: Rejects booking past, inactive, or already-booked slots")
    void testBookingValidationRejections() {
        User user = new User("Alice", "alice@example.com", null, Role.USER);
        user.setId(5L);
        Service service = new Service("Consultation", "Care", 30, true);
        service.setId(1L);

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        // 1. Inactive slot
        AppointmentSlot inactiveSlot = new AppointmentSlot(service, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1), false);
        inactiveSlot.setId(1L);
        when(slotRepository.findById(1L)).thenReturn(Optional.of(inactiveSlot));

        assertThatThrownBy(() -> appointmentService.bookAppointment(new AppointmentBookingRequest(5L, 1L, "Check")))
                .isInstanceOf(InvalidAppointmentOperationException.class)
                .hasMessageContaining("inactive");

        // 2. Past slot
        AppointmentSlot pastSlot = new AppointmentSlot(service, LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1).plusHours(1), true);
        pastSlot.setId(2L);
        when(slotRepository.findById(2L)).thenReturn(Optional.of(pastSlot));

        assertThatThrownBy(() -> appointmentService.bookAppointment(new AppointmentBookingRequest(5L, 2L, "Check")))
                .isInstanceOf(InvalidAppointmentOperationException.class)
                .hasMessageContaining("past");

        // 3. Already booked slot
        AppointmentSlot bookedSlot = new AppointmentSlot(service, LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(1), true);
        bookedSlot.setId(3L);
        when(slotRepository.findById(3L)).thenReturn(Optional.of(bookedSlot));
        when(appointmentRepository.existsBySlotIdAndStatus(3L, AppointmentStatus.BOOKED)).thenReturn(true);

        assertThatThrownBy(() -> appointmentService.bookAppointment(new AppointmentBookingRequest(5L, 3L, "Check")))
                .isInstanceOf(SlotAlreadyBookedException.class)
                .hasMessageContaining("already booked");
    }

    @Test
    @DisplayName("AppointmentService: Translates DataIntegrityViolationException into SlotAlreadyBookedException")
    void testBookingDataIntegrityTranslation() {
        User user = new User("Alice", "alice@example.com", null, Role.USER);
        user.setId(5L);
        Service service = new Service("Consultation", "Care", 30, true);
        service.setId(1L);

        AppointmentSlot slot = new AppointmentSlot(service, LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(1), true);
        slot.setId(30L);

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(slotRepository.findById(30L)).thenReturn(Optional.of(slot));
        when(appointmentRepository.existsBySlotIdAndStatus(30L, AppointmentStatus.BOOKED)).thenReturn(false);

        when(appointmentRepository.saveAndFlush(any(Appointment.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key uk_active_slot_booking_token"));

        assertThatThrownBy(() -> appointmentService.bookAppointment(new AppointmentBookingRequest(5L, 30L, "Check")))
                .isInstanceOf(SlotAlreadyBookedException.class)
                .hasMessageContaining("already been booked by another request");
    }

    @Test
    @DisplayName("AppointmentService: Cancellation succeeds by owner and fails by unauthorized user")
    void testAppointmentCancellationPermissions() {
        User owner = new User("Owner User", "owner@example.com", null, Role.USER);
        owner.setId(10L);

        Service service = new Service("Consultation", "Care", 30, true);
        AppointmentSlot slot = new AppointmentSlot(service, LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(1), true);
        slot.setId(15L);

        Appointment appt = new Appointment(owner, slot, AppointmentStatus.BOOKED, "Initial reason");
        appt.setId(50L);

        when(appointmentRepository.findById(50L)).thenReturn(Optional.of(appt));

        // 1. Rejection by different user
        assertThatThrownBy(() -> appointmentService.cancelAppointment(50L, 999L, new AppointmentCancelRequest("Not my appt")))
                .isInstanceOf(UnauthorizedAppointmentAccessException.class)
                .hasMessageContaining("not authorized");

        // 2. Success by owner
        when(appointmentRepository.saveAndFlush(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse cancelled = appointmentService.cancelAppointment(50L, 10L, new AppointmentCancelRequest("Personal conflict"));
        assertThat(cancelled.status()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(appt.getActiveSlotToken()).isNull();
    }
}
