package com.haircutbooking.Haircut_Booking.domain.RequestDTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
public class ModifyUserRequest {
    @Email(message = "Invalid Email")
    private String email;

    @NotBlank(message = "Invalid Phone number")
    private String sdt;

    @NotBlank(message = "Invalid full Name")
    private String hoTen;

}
