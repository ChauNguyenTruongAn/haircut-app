package com.haircutbooking.Haircut_Booking.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.haircutbooking.Haircut_Booking.domain.AppointmentServiceId;
import com.haircutbooking.Haircut_Booking.domain.AppointmentServices;

public interface AppointmentServicesRepository extends JpaRepository<AppointmentServices, AppointmentServiceId> {
    @Transactional
    @Modifying
    @Query("DELETE FROM AppointmentServices a WHERE a.appointment.id = :appointmentId")
    int deleteByAppointmentId(@Param("appointmentId") Long appointmentId);

    Optional<AppointmentServices> findById(AppointmentServiceId appointmentServiceId);
}
