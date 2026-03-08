package com.haircutbooking.Haircut_Booking.domain.RequestDTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ChangePasswordRequest {
    @NotBlank
    private String password;
}
