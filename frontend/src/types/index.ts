export type Role = 'USER' | 'ADMIN';

export type AppointmentStatus = 'BOOKED' | 'CANCELLED';

export interface User {
  id: number;
  fullName: string;
  email: string;
  role: Role;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  userId: number;
  email: string;
  fullName: string;
  role: Role;
}

export interface ServiceItem {
  id: number;
  name: string;
  description: string;
  durationMinutes: number;
  active: boolean;
  createdAt: string;
}

export interface AppointmentSlotItem {
  id: number;
  serviceId: number;
  serviceName: string;
  serviceDurationMinutes: number;
  startTime: string;
  endTime: string;
  active: boolean;
  createdAt: string;
}

export interface AppointmentItem {
  id: number;
  userId: number;
  userFullName: string;
  userEmail: string;
  slotId: number;
  serviceId: number;
  serviceName: string;
  startTime: string;
  endTime: string;
  status: AppointmentStatus;
  reason?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  validationErrors?: { field: string; message: string }[];
}

export interface HealthResponse {
  status: string;
  service: string;
  timestamp: string;
}
