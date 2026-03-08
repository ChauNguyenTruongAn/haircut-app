package com.haircutbooking.Haircut_Booking.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.haircutbooking.Haircut_Booking.domain.Barber;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityDTO {
    private LocalDate date;
    private LocalTime time;
    private Boolean isAvailable;
    private Integer barbersAvailable;
    private LocalTime nextAvailableTime;
    private List<Barber> availableBarbers;
}