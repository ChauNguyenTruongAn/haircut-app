package com.haircutbooking.Haircut_Booking.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TimeSlotDTO is now a simplified representation of available time slots
 * It no longer directly links to a database entity since time_slots table has
 * been removed
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotDTO {
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean available;
    private Long barberId;
    private String barberName;
}