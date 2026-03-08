package com.haircutbooking.Haircut_Booking.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDTO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal basePrice;
    private Integer durationMinutes;
    private String imageUrl;
    private Boolean isActive;
    private Integer sortOrder;
}