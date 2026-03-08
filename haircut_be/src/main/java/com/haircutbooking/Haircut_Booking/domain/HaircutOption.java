package com.haircutbooking.Haircut_Booking.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Đại diện cho một dịch vụ cắt tóc hoặc làm đẹp
 */
@Entity
@Table(name = "haircut_services")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HaircutOption extends AbstractEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private BigDecimal basePrice;

    @Column(nullable = false, name = "duration_minutes")
    private Integer durationMinutes; // Thời gian tính bằng phút

    @Column(length = 255)
    private String imageUrl;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}