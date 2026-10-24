package com.appointment.booking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "appointments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_active_slot_booking_token",
                        columnNames = {"active_slot_token"}
                )
        },
        indexes = {
                @Index(name = "idx_appointments_user_status", columnList = "user_id, status"),
                @Index(name = "idx_appointments_slot", columnList = "slot_id"),
                @Index(name = "idx_appointments_status", columnList = "status")
        }
)
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "User reference is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_appointments_user")
    )
    private User user;

    @NotNull(message = "Appointment slot is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "slot_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_appointments_slot")
    )
    private AppointmentSlot slot;

    @NotNull(message = "Appointment status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AppointmentStatus status = AppointmentStatus.BOOKED;

    @Size(max = 255, message = "Reason cannot exceed 255 characters")
    @Column(name = "reason", length = 255)
    private String reason;

    /**
     * Concurrency & Deduplication Token:
     * When status == BOOKED, activeSlotToken is set to "SLOT_" + slot.getId().
     * Because uk_active_slot_booking_token enforces uniqueness, MySQL will reject
     * concurrent duplicate active bookings for the same slot at the database engine level (ACID).
     * When status == CANCELLED, this token is set to NULL (or a unique cancellation token),
     * allowing subsequent bookings if business rules allow or keeping history safely.
     */
    @Column(name = "active_slot_token", length = 64)
    private String activeSlotToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Appointment() {
    }

    public Appointment(User user, AppointmentSlot slot, AppointmentStatus status, String reason) {
        this.user = user;
        this.slot = slot;
        this.status = status != null ? status : AppointmentStatus.BOOKED;
        this.reason = reason;
        syncActiveSlotToken();
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
        syncActiveSlotToken();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        syncActiveSlotToken();
    }

    public void syncActiveSlotToken() {
        if (this.status == AppointmentStatus.BOOKED && this.slot != null && this.slot.getId() != null) {
            this.activeSlotToken = "SLOT_" + this.slot.getId();
        } else if (this.status == AppointmentStatus.CANCELLED) {
            this.activeSlotToken = null;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public AppointmentSlot getSlot() {
        return slot;
    }

    public void setSlot(AppointmentSlot slot) {
        this.slot = slot;
        syncActiveSlotToken();
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
        syncActiveSlotToken();
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getActiveSlotToken() {
        return activeSlotToken;
    }

    public void setActiveSlotToken(String activeSlotToken) {
        this.activeSlotToken = activeSlotToken;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
