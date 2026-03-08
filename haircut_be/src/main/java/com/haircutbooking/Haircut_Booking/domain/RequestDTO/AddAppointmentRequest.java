package com.haircutbooking.Haircut_Booking.domain.RequestDTO;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.haircutbooking.Haircut_Booking.util.AppointmentStatus;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AddAppointmentRequest {

    @NotNull(message = "User ID cannot be null")
    private Long userId;

    @NotNull(message = "Barber ID cannot be null")
    private Long barberId;

    @NotNull(message = "Appointment date cannot be null")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate appointmentDate;

    @NotNull(message = "Appointment start time cannot be null")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime appointmentTime;

    @NotNull(message = "Appointment end time cannot be null")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime appointmentTimeEnd;

    @NotNull(message = "Status cannot be null")
    private AppointmentStatus status;

    @Size(max = 500, message = "Note should be less than 500 characters")
    private String note;

    @NotEmpty(message = "Service IDs and quantities are required")
    private Map<Long, Integer> services; // Map<ServiceId, Quantity>
}
