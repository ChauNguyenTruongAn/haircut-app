package com.haircutbooking.Haircut_Booking.domain;

import com.haircutbooking.Haircut_Booking.util.NotificationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "noti_user")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotiUser {
    @EmbeddedId
    private NotiUserId id;

    @Builder.Default
    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    private NotificationStatus status = NotificationStatus.UNREAD;

    @MapsId("user_id")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @MapsId("noti_id")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "noti_id", nullable = false)
    private Notification noti;
}
