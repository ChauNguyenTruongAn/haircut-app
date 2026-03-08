package com.haircutbooking.Haircut_Booking.domain;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class RolePerId {
    private Long role_id;
    private Long per_id;
}
