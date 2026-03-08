package com.haircutbooking.Haircut_Booking.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "appointment_services")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AppointmentServices {
    @EmbeddedId
    private AppointmentServiceId id;

    private int quantity;

    @MapsId("service_id")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    @JsonBackReference
    private HaircutOption service;

    @MapsId("appointment_id")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    @JsonBackReference
    private Appointment appointment;
}
