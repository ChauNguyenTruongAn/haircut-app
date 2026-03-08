package com.haircutbooking.Haircut_Booking.domain.RequestDTO;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.haircutbooking.Haircut_Booking.util.AppointmentStatus;

@Data
public class UpdateAppointmentStatusRequest {

    @NotNull(message = "Status is required")
    private AppointmentStatus status;

    private String reason; // Optional reason for cancellation
}