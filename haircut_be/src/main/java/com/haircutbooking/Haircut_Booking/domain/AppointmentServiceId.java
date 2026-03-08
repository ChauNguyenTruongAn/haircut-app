package com.haircutbooking.Haircut_Booking.domain;

import java.io.Serializable;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentServiceId implements Serializable {
    private Long appointment_id;
    private Long service_id;
}
