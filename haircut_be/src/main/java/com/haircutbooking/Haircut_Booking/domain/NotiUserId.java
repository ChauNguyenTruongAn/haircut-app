package com.haircutbooking.Haircut_Booking.domain;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Embeddable
public class NotiUserId {
    private Long user_id;
    private Long noti_id;
}
