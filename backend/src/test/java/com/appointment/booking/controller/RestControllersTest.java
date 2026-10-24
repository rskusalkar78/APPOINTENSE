package com.appointment.booking.controller;

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
import com.appointment.booking.exception.SlotAlreadyBookedException;
import com.appointment.booking.exception.UnauthorizedAppointmentAccessException;
import com.appointment.booking.model.AppointmentStatus;
import com.appointment.booking.model.Role;
import com.appointment.booking.security.UserPrincipal;
import com.appointment.booking.service.AppointmentService;
import com.appointment.booking.service.AppointmentSlotService;
import com.appointment.booking.service.ServiceManagementService;
import com.appointment.booking.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RestControllersTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ServiceManagementService serviceManagementService;

    @MockitoBean
    private AppointmentSlotService appointmentSlotService;

    @MockitoBean
    private AppointmentService appointmentService;

    // --- Helper: create a UserPrincipal with a given role ---
    private UserPrincipal userPrincipal(Long id, String email, String name, Role role) {
        String authority = "ROLE_" + role.name();
        return new UserPrincipal(
                id, email, name, "encodedPassword", role,
                Collections.singletonList(new SimpleGrantedAuthority(authority))
        );
    }

    // --- USERS ENDPOINTS ---
    @Test
    @DisplayName("POST /api/users/register returns 201 Created on valid input (public)")
    void testRegisterUserSuccess() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest("John Doe", "john@example.com", "securePass123");
        UserResponse response = new UserResponse(1L, "John Doe", "john@example.com", Role.USER, LocalDateTime.now());

        when(userService.registerUser(any(UserRegistrationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    @DisplayName("POST /api/users/register returns 409 Conflict when duplicate email")
    void testRegisterUserDuplicateConflict() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest("John Doe", "john@example.com", "securePass123");
        when(userService.registerUser(any(UserRegistrationRequest.class)))
                .thenThrow(new DuplicateResourceException("An account with email john@example.com already exists"));

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("An account with email john@example.com already exists"));
    }

    @Test
    @DisplayName("POST /api/users/register returns 400 Bad Request on invalid email")
    void testRegisterUserValidationFailure() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest("J", "invalid-email", "pass");

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors").isArray());
    }

    @Test
    @DisplayName("GET /api/users/{id} returns 200 OK for own profile, 403 for other's profile")
    void testGetUserById() throws Exception {
        UserPrincipal principal = userPrincipal(10L, "alice@example.com", "Alice", Role.USER);
        UserResponse response = new UserResponse(10L, "Alice", "alice@example.com", Role.USER, LocalDateTime.now());
        when(userService.getUserById(10L)).thenReturn(response);

        // Own profile — 200 OK
        mockMvc.perform(get("/api/users/10")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Alice"));

        // Other user's profile — 403 Forbidden
        mockMvc.perform(get("/api/users/99")
                        .with(user(principal)))
                .andExpect(status().isForbidden());
    }

    // --- SERVICES ENDPOINTS ---
    @Test
    @DisplayName("POST /api/services returns 201 Created (ADMIN) and GET /api/services returns 200 OK (public)")
    void testServiceEndpoints() throws Exception {
        ServiceCreateRequest createReq = new ServiceCreateRequest("Cardiology", "Heart exam", 45, true);
        ServiceResponse res = new ServiceResponse(1L, "Cardiology", "Heart exam", 45, true, LocalDateTime.now());

        when(serviceManagementService.createService(any(ServiceCreateRequest.class))).thenReturn(res);
        when(serviceManagementService.getActiveServices()).thenReturn(List.of(res));

        // POST requires ADMIN role
        UserPrincipal adminPrincipal = userPrincipal(1L, "admin@example.com", "Admin", Role.ADMIN);

        mockMvc.perform(post("/api/services")
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Cardiology"));

        // GET is public — no auth needed
        mockMvc.perform(get("/api/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Cardiology"));
    }

    // --- SLOTS ENDPOINTS ---
    @Test
    @DisplayName("POST /api/slots returns 201 Created (ADMIN) and rejects invalid time bounds")
    void testSlotEndpoints() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        LocalDateTime end = start.plusMinutes(30);
        SlotCreateRequest request = new SlotCreateRequest(1L, start, end, true);
        SlotResponse res = new SlotResponse(100L, 1L, "Cardiology", 45, start, end, true, LocalDateTime.now());

        when(appointmentSlotService.createSlot(any(SlotCreateRequest.class))).thenReturn(res);

        // POST requires ADMIN role
        UserPrincipal adminPrincipal = userPrincipal(1L, "admin@example.com", "Admin", Role.ADMIN);

        mockMvc.perform(post("/api/slots")
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L));

        // GET /api/slots with invalid date range: from after to (public endpoint)
        mockMvc.perform(get("/api/slots")
                        .param("from", "2026-10-30T10:00:00")
                        .param("to", "2026-10-25T10:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("'to' time cannot be earlier than 'from' time"));
    }

    // --- APPOINTMENTS ENDPOINTS ---
    @Test
    @DisplayName("POST /api/appointments returns 201 Created on success and 409 Conflict when already booked")
    void testBookAppointmentEndpoints() throws Exception {
        // The controller derives userId from the authenticated principal, so the request userId is overridden
        AppointmentBookingRequest bookingReq = new AppointmentBookingRequest(1L, 100L, "Routine checkup");
        AppointmentResponse res = new AppointmentResponse(
                50L, 1L, "John", "john@example.com", 100L, 1L, "Cardiology",
                LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusMinutes(30),
                AppointmentStatus.BOOKED, "Routine checkup", LocalDateTime.now(), LocalDateTime.now()
        );

        when(appointmentService.bookAppointment(any(AppointmentBookingRequest.class))).thenReturn(res);

        UserPrincipal principal = userPrincipal(1L, "john@example.com", "John", Role.USER);

        mockMvc.perform(post("/api/appointments")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(50L))
                .andExpect(jsonPath("$.status").value("BOOKED"));

        // Conflict check
        when(appointmentService.bookAppointment(any(AppointmentBookingRequest.class)))
                .thenThrow(new SlotAlreadyBookedException("Slot id 100 is already booked"));

        mockMvc.perform(post("/api/appointments")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Slot id 100 is already booked"));
    }

    @Test
    @DisplayName("POST /api/appointments/{id}/cancel returns 200 OK or 403 Forbidden on unauthorized user")
    void testCancelAppointmentEndpoint() throws Exception {
        AppointmentCancelRequest cancelReq = new AppointmentCancelRequest("Personal conflict");
        AppointmentResponse res = new AppointmentResponse(
                50L, 1L, "John", "john@example.com", 100L, 1L, "Cardiology",
                LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusMinutes(30),
                AppointmentStatus.CANCELLED, "Routine [Cancelled: Personal conflict]",
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(appointmentService.cancelAppointment(eq(50L), eq(1L), any(AppointmentCancelRequest.class)))
                .thenReturn(res);

        UserPrincipal ownerPrincipal = userPrincipal(1L, "john@example.com", "John", Role.USER);

        // Success by owner
        mockMvc.perform(post("/api/appointments/50/cancel")
                        .with(user(ownerPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Forbidden by different user
        when(appointmentService.cancelAppointment(eq(50L), eq(999L), any(AppointmentCancelRequest.class)))
                .thenThrow(new UnauthorizedAppointmentAccessException("User id 999 is not authorized to cancel appointment id 50"));

        UserPrincipal otherPrincipal = userPrincipal(999L, "other@example.com", "Other", Role.USER);

        mockMvc.perform(post("/api/appointments/50/cancel")
                        .with(user(otherPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("User id 999 is not authorized to cancel appointment id 50"));
    }
}
