package com.haircutbooking.Haircut_Booking.domain.RequestDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String username;

    private String password;

    private String email;

    private String sdt;

    private String hoTen;
}
