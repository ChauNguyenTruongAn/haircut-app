package com.haircutbooking.Haircut_Booking.domain.RequestDTO;

import java.math.BigDecimal;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddServiceRequest {

    @NotNull(message = "Service name is required")
    private String name;

    private String description;

    @NotNull(message = "Duration in minutes is required")
    private Integer durationMinutes;

    @NotNull(message = "Base price is required")
    private BigDecimal basePrice;

    private String imageUrl;

    private Integer sortOrder;
}
