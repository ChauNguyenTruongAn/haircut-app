package com.haircutbooking.Haircut_Booking.repositories;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.haircutbooking.Haircut_Booking.domain.Barber;

@Repository
public interface BarberRepository extends JpaRepository<Barber, Long> {

        List<Barber> findByIsActiveTrueOrderByNameAsc();

        Optional<Barber> findByIdAndIsActiveTrue(Long id);

        @Query("SELECT b FROM Barber b WHERE b.isActive = true AND b.isAvailableForBooking = true ORDER BY b.name")
        List<Barber> findAllAvailableBarbers();

        @Query("SELECT DISTINCT b FROM Barber b " +
                        "JOIN b.services s " +
                        "WHERE b.isActive = true AND b.isAvailableForBooking = true " +
                        "AND s.id = :serviceId")
        List<Barber> findBarbersOfferingService(@Param("serviceId") Long serviceId);

        @Query("SELECT DISTINCT b FROM Barber b " +
                        "WHERE b.isActive = true AND b.isAvailableForBooking = true " +
                        "AND :time BETWEEN b.startWorkingHour AND b.endWorkingHour " +
                        "AND NOT EXISTS (" +
                        "   SELECT a FROM Appointment a " +
                        "   WHERE a.barber = b " +
                        "   AND a.date = :date " +
                        "   AND a.status NOT IN ('CANCELLED') " +
                        "   AND ((a.startTime <= :time AND a.endTime > :time) OR " +
                        "        (:time <= a.startTime AND :time >= a.startTime)) " +
                        ") " +
                        "ORDER BY b.name")
        List<Barber> findAvailableBarbersAtDateTime(@Param("date") LocalDate date, @Param("time") LocalTime time);
}
