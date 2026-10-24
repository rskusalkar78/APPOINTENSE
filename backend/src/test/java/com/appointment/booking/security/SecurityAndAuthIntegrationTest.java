package com.appointment.booking.security;

import com.appointment.booking.dto.request.AppointmentBookingRequest;
import com.appointment.booking.dto.request.AppointmentCancelRequest;
import com.appointment.booking.dto.request.ServiceCreateRequest;
import com.appointment.booking.dto.request.SlotCreateRequest;
import com.appointment.booking.dto.request.UserLoginRequest;
import com.appointment.booking.dto.request.UserRegistrationRequest;
import com.appointment.booking.dto.response.AppointmentResponse;
import com.appointment.booking.dto.response.AuthResponse;
import com.appointment.booking.dto.response.ServiceResponse;
import com.appointment.booking.dto.response.SlotResponse;
import com.appointment.booking.dto.response.UserResponse;
import com.appointment.booking.exception.DuplicateResourceException;
import com.appointment.booking.exception.ResourceNotFoundException;
import com.appointment.booking.exception.SlotAlreadyBookedException;
import com.appointment.booking.model.AppointmentStatus;
import com.appointment.booking.model.Role;
import com.appointment.booking.service.AppointmentService;
import com.appointment.booking.service.AppointmentSlotService;
import com.appointment.booking.service.AuthService;
import com.appointment.booking.service.ServiceManagementService;
import com.appointment.booking.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAndAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ServiceManagementService serviceManagementService;

    @MockitoBean
    private AppointmentSlotService appointmentSlotService;

    @MockitoBean
    private AppointmentService appointmentService;

    @Test
    @DisplayName("Public: GET /api/health is accessible without authentication")
    void testPublicHealth() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Public: POST /api/users/register allows anonymous user registration")
    void testPublicRegister() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest("New Patient", "newpatient@example.com", "SecurePassword123");
        UserResponse response = new UserResponse(100L, "New Patient", "newpatient@example.com", Role.USER, LocalDateTime.now());

        when(userService.registerUser(any(UserRegistrationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("Auth: POST /api/auth/login returns JWT token with valid credentials")
    void testLoginSuccess() throws Exception {
        UserLoginRequest loginReq = new UserLoginRequest("user@example.com", "correctPassword123");
        String testToken = jwtProvider.generateToken(1L, "user@example.com", Role.USER);
        AuthResponse authRes = AuthResponse.of(testToken, 3600000, 1L, "user@example.com", "Regular User", Role.USER);

        when(authService.login(any(UserLoginRequest.class))).thenReturn(authRes);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(testToken))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("Auth: POST /api/auth/login returns 401 Unauthorized with bad credentials")
    void testLoginFailure() throws Exception {
        UserLoginRequest loginReq = new UserLoginRequest("user@example.com", "wrongPassword");
        when(authService.login(any(UserLoginRequest.class))).thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Security: Protected endpoint returns 401 when accessed anonymously")
    void testProtectedEndpointAnonymous401() throws Exception {
        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slotId\":1,\"reason\":\"Check\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Security: Protected endpoint returns 401 with malformed or invalid token")
    void testProtectedEndpointInvalidToken401() throws Exception {
        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer invalid.token.value")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slotId\":1,\"reason\":\"Check\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Role Authorization: USER cannot create service (returns 403 Forbidden)")
    void testUserCannotCreateService() throws Exception {
        String userToken = jwtProvider.generateToken(5L, "user5@example.com", Role.USER);
        ServiceCreateRequest req = new ServiceCreateRequest("Pediatrics", "Child care", 30, true);

        mockMvc.perform(post("/api/services")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Role Authorization: ADMIN can create service (returns 201 Created)")
    void testAdminCanCreateService() throws Exception {
        String adminToken = jwtProvider.generateToken(99L, "admin@example.com", Role.ADMIN);
        ServiceCreateRequest req = new ServiceCreateRequest("Cardiology", "Heart check", 45, true);
        ServiceResponse res = new ServiceResponse(1L, "Cardiology", "Heart check", 45, true, LocalDateTime.now());

        when(serviceManagementService.createService(any(ServiceCreateRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/services")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Cardiology"));
    }

    @Test
    @DisplayName("Ownership: USER cannot view another user's appointment history (403 Forbidden)")
    void testUserCannotViewOtherUserAppointments() throws Exception {
        // User with ID 5 attempts to query appointments of User 9
        String user5Token = jwtProvider.generateToken(5L, "user5@example.com", Role.USER);

        mockMvc.perform(get("/api/appointments/user/9")
                        .header("Authorization", "Bearer " + user5Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You are not authorized to view appointments for user id: 9"));
    }

    @Test
    @DisplayName("Ownership: USER can view their own appointment history (200 OK)")
    void testUserCanViewOwnAppointments() throws Exception {
        String user5Token = jwtProvider.generateToken(5L, "user5@example.com", Role.USER);
        AppointmentResponse appt = new AppointmentResponse(
                1L, 5L, "User Five", "user5@example.com", 10L, 1L, "Cardiology",
                LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusMinutes(30),
                AppointmentStatus.BOOKED, "Regular Checkup", LocalDateTime.now(), LocalDateTime.now()
        );

        when(appointmentService.getAppointmentsByUser(eq(5L), any())).thenReturn(List.of(appt));

        mockMvc.perform(get("/api/appointments/user/5")
                        .header("Authorization", "Bearer " + user5Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(5L));
    }
}
