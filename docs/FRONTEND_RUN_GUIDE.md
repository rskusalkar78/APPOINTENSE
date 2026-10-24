# APPOINTENSE — Frontend & Full-Stack Local Execution Guide

This document describes how to configure, run, and demonstrate the Online Appointment Booking System locally.

---

## Architecture Overview

- **Backend**: Spring Boot 3.4.3, Java 21, Spring Data JPA, Spring Security, BCrypt, HMAC-SHA256 JWT, MySQL 8.0 (Port `8080`).
- **Frontend**: React 19, TypeScript, Vite, Tailwind CSS, React Router 7, Axios (Port `5173`).
- **Communication**: REST API over JSON with Axios interceptor providing `Authorization: Bearer <token>`.
- **CORS & Proxying**: Vite dev server proxies `/api` to `http://localhost:8080`, and Spring Security includes a local `CorsConfigurationSource` allowing `http://localhost:5173`.

---

## 1. Prerequisites Checklist

Before running the application:
1. **Java 21**: Verify via `java -version`.
2. **Node.js 18+ & npm**: Verify via `node -v` and `npm -v`.
3. **MySQL 8.0 Running**: Verify the Windows service is running:
   ```powershell
   Get-Service -Name MySQL80
   ```
4. **Database Created**: Make sure the target database exists in MySQL:
   ```sql
   CREATE DATABASE IF NOT EXISTS appointment_booking;
   ```

---

## 2. Running the Backend

From the project root:

```powershell
cd backend
```

Set your MySQL root password (if configured in MySQL):
```powershell
$env:DB_PASSWORD="your_mysql_password"
```

Start the Spring Boot server using the Maven Wrapper:
```powershell
.\mvnw.cmd spring-boot:run
```

The server starts on `http://localhost:8080`.
Verify it is active by opening: `http://localhost:8080/api/health`

---

## 3. Running the Frontend

In a **separate** PowerShell terminal from the project root:

```powershell
cd frontend
npm run dev
```

The Vite dev server will output:
```
  VITE v8.3.5  ready in 320 ms

  ➜  Local:   http://localhost:5173/
  ➜  Network: use --host to expose
```

Open **`http://localhost:5173`** in your browser.

---

## 4. Testing Commands

### Backend Automated Test Suite (38 tests)
```powershell
cd backend
.\mvnw.cmd test
```
*Note: Uses an in-memory H2 database; does not depend on an active MySQL connection.*

### Frontend Production Build
```powershell
cd frontend
npm run build
```
*Compiles TypeScript and bundles assets with Vite.*

---

## 5. Demonstration Walkthrough Steps

### A. Public Browsing
1. Open `http://localhost:5173/`.
2. Click **Browse Services** or navigate to `/services` to see active consultations and durations.
3. Click **Available Slots** or navigate to `/slots` to view bookable slots with optional service and date range filtering.

### B. User Registration & Login
1. Click **Get Started** or navigate to `/register`.
2. Fill in:
   - Full Name: `Jane Doe`
   - Email: `jane@example.com`
   - Password: `Password123` (minimum 8 characters)
3. Upon registration, you are redirected to `/login`. Sign in to receive an HMAC-SHA256 JWT.

### C. Booking an Appointment
1. Navigate to `/slots`.
2. Select an open slot and click **Book This Slot**.
3. In the modal, provide an optional consultation reason and click **Confirm Booking**.
4. The slot availability will automatically refresh, and the slot will be marked as booked.

### D. Concurrency & Race-Condition Safety Demo
- If two users open the same slot and click **Confirm Booking** at the same time:
  - User 1's booking succeeds.
  - User 2 receives an instant, user-friendly HTTP 409 conflict message: *"This slot was just booked by another user. Please choose another slot."*

### E. User Dashboard & Cancellation
1. Navigate to `/dashboard` to view upcoming appointments, lifetime stats, and recent activity.
2. Navigate to `/my-appointments` to filter appointments by `ALL`, `BOOKED`, or `CANCELLED`.
3. Click **Cancel Appointment** to cancel a booking with an optional cancellation reason.

### F. Admin Portal Access
1. Promote your registered account in MySQL:
   ```sql
   UPDATE appointment_booking.users SET role = 'ADMIN' WHERE email = 'jane@example.com';
   ```
2. Log out and log back in on the website to receive an updated token with `ROLE_ADMIN`.
3. The **Admin Portal** link will appear in the navigation bar:
   - **All Bookings**: Monitor every appointment booked across the entire organization.
   - **Manage Services**: Add a new consultation service.
   - **Manage Slots**: Schedule and publish new appointment slots.
