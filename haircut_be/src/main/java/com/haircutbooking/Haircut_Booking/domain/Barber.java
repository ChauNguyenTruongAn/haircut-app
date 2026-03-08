package com.haircutbooking.Haircut_Booking.domain;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Đại diện cho một thợ cắt tóc
 */
@Entity
@Table(name = "barbers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Barber extends AbstractEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String bio;

    @Column(length = 255)
    private String avatarUrl;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(nullable = false)
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startWorkingHour;

    @Column(nullable = false)
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endWorkingHour;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(length = 30)
    private String position; // "Senior Stylist", "Junior Stylist", "Master Barber", etc.

    @Builder.Default
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "barber")
    @JsonBackReference
    private Set<Appointment> appointments = new HashSet<>();

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "barber_services", joinColumns = @JoinColumn(name = "barber_id"), inverseJoinColumns = @JoinColumn(name = "service_id"))
    private Set<HaircutOption> services = new HashSet<>();

    @Builder.Default
    @Column(nullable = false)
    private Boolean isAvailableForBooking = true;
}
