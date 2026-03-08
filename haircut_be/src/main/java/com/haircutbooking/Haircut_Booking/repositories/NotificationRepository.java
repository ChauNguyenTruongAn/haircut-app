package com.haircutbooking.Haircut_Booking.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.haircutbooking.Haircut_Booking.domain.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

}
