package com.haircutbooking.Haircut_Booking.domain.RequestDTO;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddBarberRequest {
    @NotNull(message = "Name's not null")
    @NotBlank(message = "Name's not blank")
    private String name;

    @NotNull(message = "Start working time is not null")
    @JsonFormat(pattern = "HH:mm") // because jackson not support convert json to local time
    private LocalTime startWorkingTime;

    @NotNull(message = "End working time is not null")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endWorkingTime;
}
