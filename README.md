# APPOINTENSE — Online Appointment Booking System

A full-stack, enterprise-grade appointment booking platform designed as a college mini project. Built using **Java 21**, **Spring Boot 3.4.3**, **Spring Data JPA**, **Spring Security (JWT & BCrypt)**, **MySQL 8.0**, **React 19**, **TypeScript**, **Vite**, and **Tailwind CSS**.

---

## Architecture & Technology Stack

| Layer | Technologies & Libraries |
|---|---|
| **Backend** | Java 21, Spring Boot 3.4.3, Spring Web, Spring Data JPA, Spring Security, Jakarta Validation |
| **Security** | BCrypt password hashing, HMAC-SHA256 JWT, Stateless Session, Role-Based Access Control (`USER`, `ADMIN`) |
| **Database** | MySQL 8.0, Hibernate ORM, In-memory H2 (for isolated test execution) |
| **Concurrency** | Unique constraint token pattern (`active_slot_token`) to guarantee zero duplicate bookings under race conditions |
| **Frontend** | React 19, TypeScript, Vite, Tailwind CSS, React Router 7, Axios, Lucide Icons |
| **API Integration** | RESTful JSON API with Axios interceptors attaching `Authorization: Bearer <token>` and handling auto-logout on 401 |

---

## Project Structure

```
APPOINTENSE/
├── backend/                                # Spring Boot 3.4.3 Maven project
│   ├── src/main/java/com/appointment/booking/
│   │   ├── controller/                     # REST controllers (Health, Auth, User, Service, Slot, Appointment, Admin)
│   │   ├── dto/                            # Request & Response record DTOs
│   │   ├── exception/                      # Custom exceptions & @RestControllerAdvice GlobalExceptionHandler
│   │   ├── model/                          # JPA Entities (User, Service, AppointmentSlot, Appointment, Role, AppointmentStatus)
│   │   ├── repository/                     # Spring Data JPA Repositories
│   │   ├── security/                       # SecurityConfig, JwtProvider, JwtAuthenticationFilter, UserPrincipal
│   │   └── service/                        # Service interfaces and implementations
│   ├── src/main/resources/
│   │   └── application.properties          # Production/local MySQL config with env overrides
│   ├── src/test/                           # 38 Automated unit, slice, concurrency & integration tests
│   ├── .env.example                        # Template environment variables for local backend
│   └── mvnw.cmd                            # Windows Maven Wrapper
│
├── frontend/                               # React + TypeScript + Vite project
│   ├── src/
│   │   ├── components/                     # Navbar, Layout, ProtectedRoute
│   │   ├── context/                        # AuthContext (state, login, logout, roles)
│   │   ├── pages/                          # Home, Login, Register, Services, Slots, Dashboard, MyAppointments, Admin
│   │   ├── services/                       # Axios client with interceptors
│   │   └── types/                          # TypeScript interfaces matching backend DTOs
│   ├── tailwind.config.js                  # Tailwind design system configuration
│   └── vite.config.ts                      # Vite configuration with /api reverse proxy to port 8080
│
└── docs/
    ├── API.md                              # Comprehensive REST API endpoint reference
    └── FRONTEND_RUN_GUIDE.md               # Step-by-step local execution & demo walkthrough
```

---

## Prerequisites

1. **Operating System**: Windows (or macOS / Linux with equivalent shell commands).
2. **Java Development Kit (JDK)**: Java 21 (`java -version`).
3. **Node.js & npm**: Node.js 18+ (tested on Node.js 22.17.0 and npm 10.9.2).
4. **MySQL Database**: MySQL 8.0 Server running locally (Windows service `MySQL80`).
5. **Maven**: Uses included Maven Wrapper (`mvnw.cmd`); no global Maven installation required.

---

## Local Setup & Execution Guide

### Step 1: Database Setup (MySQL 8.0)
Log into your local MySQL CLI or MySQL Workbench as `root` and create the schema:
```sql
CREATE DATABASE IF NOT EXISTS appointment_booking;
```

### Step 2: Configure Backend Environment
Optionally create `backend/.env` or set environment variables in your terminal. By default, `application.properties` provides sensible local development defaults:
- `SERVER_PORT`: `8080` (default)
- `DB_HOST`: `localhost` (default)
- `DB_PORT`: `3306` (default)
- `DB_NAME`: `appointment_booking` (default)
- `DB_USERNAME`: `root` (default)
- `DB_PASSWORD`: Set your MySQL root password (e.g., in Windows PowerShell: `$env:DB_PASSWORD="your_password"`)
- `JWT_SECRET`: A secure key of 32+ characters (has a local fallback for instant dev use)

### Step 3: Run the Backend
Open a PowerShell terminal:
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```
Verify the backend is running at: `http://localhost:8080/api/health`.

### Step 4: Run the Frontend
Open a second PowerShell terminal:
```powershell
cd frontend
npm install   # If not already installed
npm run dev
```
Open **`http://localhost:5173`** in your browser.

---

## Running Automated Tests

### Backend Regression Suite (38 tests)
The backend test suite uses an in-memory H2 database and does not require an active MySQL server connection:
```powershell
cd backend
.\mvnw.cmd test
```
**Results**:
- `HealthControllerTest`: 1 test passing
- `RestControllersTest`: 7 tests passing
- `AuthControllerTest`: 5 tests passing
- `SecurityAndAuthIntegrationTest`: 10 tests passing
- `AppointmentServiceConcurrencyIntegrationTest`: 2 tests passing (high-concurrency race condition verification)
- `ServiceLayerUnitTest`: 9 tests passing
- `AppointmentBookingBackendApplicationTests`: 1 test passing
- `RepositoryTests`: 3 tests passing
**Total: 38 Tests Run, 0 Failures, 0 Errors.**

### Frontend Production Build
```powershell
cd frontend
npm run build
```
Compiles TypeScript with zero errors and generates the production asset bundle with Vite.

---

## Key Features & User Workflows

1. **User Registration & Login**:
   - Clients validate inputs (passwords min 8 chars, email formats).
   - Backend securely hashes passwords with BCrypt before persistence.
   - Successful login issues an HMAC-SHA256 JWT stored securely for the session.
2. **Service Browsing**:
   - Explore specialized consultation types with details and expected duration in minutes.
3. **Smart Slot Selection**:
   - Filter bookable slots by service or date-time range.
   - Interactive booking dialog allows patients to specify an optional consultation reason.
4. **Concurrency & Double-Booking Prevention**:
   - Backend utilizes an atomic `active_slot_token` unique constraint.
   - If two users attempt to book the same slot simultaneously, exactly one succeeds and the other receives an HTTP `409 Conflict` message with user-friendly error guidance.
5. **My Appointments**:
   - Real-time review of scheduled consultations.
   - Self-service cancellation with optional cancellation notes.
6. **Administrator Portal** (`ROLE_ADMIN`):
   - Comprehensive dashboard of all patient appointments across the entire system.
   - Service Management: Create new medical/consultation services with specified durations.
   - Slot Scheduling: Schedule and publish new appointment slots.

---

## Promoting a User to Admin
To access the Administrator Portal during a live demo:
1. Register a regular user account on the website (e.g. `admin@example.com`).
2. Run this query in MySQL:
   ```sql
   UPDATE appointment_booking.users SET role = 'ADMIN' WHERE email = 'admin@example.com';
   ```
3. Log out and log back in on the website to receive an updated JWT containing `ROLE_ADMIN`. The **Admin Portal** link will appear in the navigation bar.
