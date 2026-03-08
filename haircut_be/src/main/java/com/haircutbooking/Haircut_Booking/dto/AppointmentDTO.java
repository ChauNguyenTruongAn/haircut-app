package com.haircutbooking.Haircut_Booking.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.haircutbooking.Haircut_Booking.domain.HaircutOption;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentDTO {
    private Long id;
    private Long userId;
    private Long barberId;
    private String barberName;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String notes;
    private Double totalPrice;
    private String senderId;

    @Builder.Default
    private Set<Long> serviceIds = new HashSet<>();

    @Builder.Default
    private List<HaircutOption> services = new ArrayList<>();

    private String status;
}