# Online Appointment Booking System - API Documentation

Base URL: `http://localhost:8080/api`

---

## 1. Authentication & Security Flow

All endpoints (except public health, public catalog browsing, user registration, and login) require a signed JSON Web Token (JWT) provided in the HTTP `Authorization` header:

```http
Authorization: Bearer <your_jwt_token_here>
```

- **Algorithm**: HMAC-SHA256
- **Token Expiration**: Configurable (Default: 24 hours / 86400000 ms)
- **Token Claims**:
  - `sub`: User email address
  - `userId`: Numeric database ID of the user
  - `role`: Role (`USER` or `ADMIN`)

---

## 2. Public Endpoints

### 1) System Health
- **Endpoint**: `GET /api/health`
- **Access**: Public
- **Response** (`200 OK`):
  ```json
  {
    "status": "UP",
    "service": "appointment-booking-backend",
    "timestamp": "2026-10-24T04:30:00.000Z"
  }
  ```

### 2) User Registration
- **Endpoint**: `POST /api/users/register`
- **Access**: Public (Always assigns `USER` role; cannot self-assign `ADMIN`)
- **Request Body**:
  ```json
  {
    "fullName": "Jane Doe",
    "email": "jane@example.com",
    "password": "StrongPassword123"
  }
  ```
- **Response** (`201 Created`):
  ```json
  {
    "id": 1,
    "fullName": "Jane Doe",
    "email": "jane@example.com",
    "role": "USER",
    "createdAt": "2026-10-24T04:31:00"
  }
  ```

### 3) User Login
- **Endpoint**: `POST /api/auth/login`
- **Access**: Public
- **Request Body**:
  ```json
  {
    "email": "jane@example.com",
    "password": "StrongPassword123"
  }
  ```
- **Response** (`200 OK`):
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "userId": 1,
    "email": "jane@example.com",
    "fullName": "Jane Doe",
    "role": "USER"
  }
  ```

### 4) Browse Active Services
- **Endpoint**: `GET /api/services`
- **Access**: Public (Read-Only)
- **Response** (`200 OK`): List of active services.

### 5) Browse Bookable Slots
- **Endpoint**: `GET /api/slots`
- **Access**: Public (Read-Only)
- **Query Parameters**:
  - `serviceId` (optional): Long
  - `from` (optional): ISO date-time, e.g. `2026-10-25T09:00:00`
  - `to` (optional): ISO date-time, e.g. `2026-10-25T17:00:00`
- **Response** (`200 OK`): List of bookable slots (active and not currently booked).

---

## 3. Authenticated USER Endpoints

### 1) View User Profile
- **Endpoint**: `GET /api/users/{id}`
- **Access**: Authenticated owner or `ADMIN`
- **Header**: `Authorization: Bearer <token>`
- **Response** (`200 OK`): `UserResponse`

### 2) Book an Appointment
- **Endpoint**: `POST /api/appointments`
- **Access**: Authenticated `USER`
- **Header**: `Authorization: Bearer <token>`
- **Request Body**:
  ```json
  {
    "slotId": 5,
    "reason": "Routine Dental Checkup"
  }
  ```
  *(Note: The user ID is automatically derived from the authenticated token principal)*
- **Response** (`201 Created`): `AppointmentResponse`

### 3) View User Appointment History
- **Endpoint**: `GET /api/appointments/user/{userId}`
- **Access**: Authenticated owner or `ADMIN`
- **Header**: `Authorization: Bearer <token>`
- **Query Parameters**:
  - `status` (optional): `BOOKED` or `CANCELLED`
- **Response** (`200 OK`): List of appointments.

### 4) Cancel an Appointment
- **Endpoint**: `POST /api/appointments/{id}/cancel`
- **Access**: Authenticated owner of the appointment
- **Header**: `Authorization: Bearer <token>`
- **Request Body** (optional):
  ```json
  {
    "cancellationReason": "Rescheduling due to travel"
  }
  ```
- **Response** (`200 OK`): Updated `AppointmentResponse` with status `CANCELLED`.

---

## 4. ADMIN-Only Endpoints

### 1) Create Service
- **Endpoint**: `POST /api/services`
- **Access**: `ROLE_ADMIN`
- **Header**: `Authorization: Bearer <admin_token>`
- **Request Body**:
  ```json
  {
    "name": "Cardiology Consultation",
    "description": "Comprehensive cardiac diagnostic review",
    "durationMinutes": 45,
    "active": true
  }
  ```
- **Response** (`201 Created`)

### 2) Create Appointment Slot
- **Endpoint**: `POST /api/slots`
- **Access**: `ROLE_ADMIN`
- **Header**: `Authorization: Bearer <admin_token>`
- **Request Body**:
  ```json
  {
    "serviceId": 1,
    "startTime": "2026-10-30T10:00:00",
    "endTime": "2026-10-30T10:45:00",
    "active": true
  }
  ```
- **Response** (`201 Created`)

### 3) View All System Appointments
- **Endpoint**: `GET /api/admin/appointments`
- **Access**: `ROLE_ADMIN`
- **Header**: `Authorization: Bearer <admin_token>`
- **Query Parameters**:
  - `status` (optional): `BOOKED` or `CANCELLED`
- **Response** (`200 OK`): List of all appointments across all users.

---

## 5. Safe Local Admin Setup

To establish the initial administrator in a local development environment:

### Option A: Manual MySQL Query
1. Register a user via `POST /api/users/register` (e.g. `admin@example.com`).
2. Run this SQL query directly in MySQL Workbench or CLI:
   ```sql
   UPDATE appointment_booking.users
   SET role = 'ADMIN'
   WHERE email = 'admin@example.com';
   ```
3. Log in via `POST /api/auth/login` to obtain a JWT containing the `ADMIN` role claim.
