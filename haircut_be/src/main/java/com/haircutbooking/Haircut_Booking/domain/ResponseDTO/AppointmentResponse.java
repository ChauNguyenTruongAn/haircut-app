package com.haircutbooking.Haircut_Booking.domain.ResponseDTO;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.haircutbooking.Haircut_Booking.domain.HaircutOption;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppointmentResponse {
    private Long userId;
    private Long barberId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate appointmentDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime appointmentTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime appointmentTimeEnd;

    private String status;

    private String note;

    private Set<HaircutOption> services;
}