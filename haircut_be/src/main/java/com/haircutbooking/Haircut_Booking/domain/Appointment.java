package com.haircutbooking.Haircut_Booking.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.haircutbooking.Haircut_Booking.util.AppointmentStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Đại diện cho một cuộc hẹn cắt tóc
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "appointments")
public class Appointment extends AbstractEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id")
    @JsonManagedReference
    private User customer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "barber_id")
    @JsonManagedReference
    private Barber barber;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false, length = 20)
    private String customerPhone;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AppointmentStatus status = AppointmentStatus.BOOKED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Column(nullable = true)
    private LocalDateTime confirmedAt;

    @Column(nullable = true)
    private LocalDateTime cancelledAt;

    @Column(nullable = true, length = 255)
    private String cancellationReason;

    @Column(nullable = true)
    private LocalDateTime completedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "appointment_services", joinColumns = @JoinColumn(name = "appointment_id"), inverseJoinColumns = @JoinColumn(name = "service_id"))
    private Set<HaircutOption> services = new HashSet<>();

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "appointment")
    private Payment payment;

    @Column(nullable = true)
    private Boolean isReminderSent;

    @Column(nullable = true)
    private LocalDateTime reminderSentAt;
}
