package com.haircutbooking.Haircut_Booking.domain;

import java.time.LocalTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlot {
    private LocalTime startTime;
    private LocalTime endTime;
}