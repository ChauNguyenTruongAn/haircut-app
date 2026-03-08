package com.haircutbooking.Haircut_Booking.repositories;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.haircutbooking.Haircut_Booking.domain.Appointment;
import com.haircutbooking.Haircut_Booking.util.AppointmentStatus;

import jakarta.transaction.Transactional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

        @Modifying
        @Transactional
        @Query("DELETE FROM AppointmentServices a WHERE a.appointment.id = :appointmentId")
        void deleteByAppointmentId(@Param("appointmentId") Long appointmentId);

        List<Appointment> findByBarberIdAndDate(Long barberId, LocalDate appointmentDate);

        Page<Appointment> findAll(Pageable pageable);

        @Query("SELECT a FROM Appointment a WHERE a.date = :date ORDER BY a.startTime")
        List<Appointment> findByDate(@Param("date") LocalDate date);

        @Query("SELECT a FROM Appointment a WHERE a.date BETWEEN :startDate AND :endDate ORDER BY a.date, a.startTime")
        List<Appointment> findByDateBetween(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT a FROM Appointment a WHERE a.barber.id = :barberId AND a.date = :date ORDER BY a.startTime")
        List<Appointment> findByBarberAndDate(@Param("barberId") Long barberId, @Param("date") LocalDate date);

        @Query("SELECT a FROM Appointment a WHERE a.barber.id = :barberId AND a.date BETWEEN :startDate AND :endDate ORDER BY a.date, a.startTime")
        List<Appointment> findByBarberAndDateBetween(
                        @Param("barberId") Long barberId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT a FROM Appointment a WHERE a.customer.id = :customerId ORDER BY a.date DESC, a.startTime DESC")
        List<Appointment> findByCustomerId(@Param("customerId") Long customerId);

        @Query("SELECT a FROM Appointment a WHERE a.customerPhone = :phone ORDER BY a.date DESC, a.startTime DESC")
        List<Appointment> findByCustomerPhone(@Param("phone") String phone);

        @Query("SELECT a FROM Appointment a WHERE a.status = :status ORDER BY a.date, a.startTime")
        List<Appointment> findByStatus(@Param("status") AppointmentStatus status);

        @Query("SELECT a FROM Appointment a WHERE a.date = :date AND a.status = :status ORDER BY a.startTime")
        List<Appointment> findByDateAndStatus(@Param("date") LocalDate date, @Param("status") AppointmentStatus status);

        @Query("SELECT a FROM Appointment a WHERE a.date > :date AND a.isReminderSent = false ORDER BY a.date, a.startTime")
        List<Appointment> findUpcomingAppointmentsForReminder(@Param("date") LocalDate date);

        @Query("SELECT COUNT(a) FROM Appointment a WHERE a.date = :date AND a.barber.id = :barberId")
        long countByDateAndBarber(@Param("date") LocalDate date, @Param("barberId") Long barberId);

        Optional<Appointment> findByIdAndStatus(Long id, AppointmentStatus status);

        /**
         * Kiểm tra xem có lịch hẹn nào vào ngày và giờ cụ thể không
         * Chỉ lấy các lịch không bị hủy (status khác CANCELLED)
         */
        @Query("SELECT a FROM Appointment a WHERE a.date = :date AND a.startTime <= :time AND a.endTime > :time AND a.status != 'CANCELLED'")
        List<Appointment> findActiveAppointmentsAtDateTime(@Param("date") LocalDate date,
                        @Param("time") LocalTime time);

        /**
         * Tìm các khung giờ được sử dụng trong ngày
         * Dùng để tìm các khung giờ trống
         */
        @Query("SELECT a.startTime FROM Appointment a WHERE a.date = :date AND a.status != 'CANCELLED' ORDER BY a.startTime")
        List<LocalTime> findUsedTimeSlotsInDay(@Param("date") LocalDate date);

        /**
         * Tìm khung giờ trống tiếp theo từ một thời điểm cụ thể
         */
        @Query("SELECT MIN(a.startTime) FROM Appointment a WHERE a.date = :date AND a.startTime > :time AND a.status != 'CANCELLED'")
        Optional<LocalTime> findNextUsedTimeSlotAfter(@Param("date") LocalDate date, @Param("time") LocalTime time);

        @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
                        "WHERE p.appointment.status = 'CONFIRMED' " +
                        "AND p.status = 'COMPLETED' " +
                        "AND p.appointment.date BETWEEN :startDate AND :endDate")
        Double getTotalRevenue(LocalDate startDate, LocalDate endDate);

        @Query("SELECT s.name, COALESCE(SUM(p.amount), 0) FROM Payment p " +
                        "JOIN p.appointment a " +
                        "JOIN AppointmentServices aps ON aps.appointment.id = a.id " +
                        "JOIN aps.service s " +
                        "WHERE a.status = 'CONFIRMED' " +
                        "AND p.status = 'COMPLETED' " +
                        "AND a.date BETWEEN :startDate AND :endDate " +
                        "GROUP BY s.name")
        List<Object[]> getRevenueByService(LocalDate startDate, LocalDate endDate);

        @Query("SELECT a.status, COUNT(a) FROM Appointment a " +
                        "WHERE a.date BETWEEN :startDate AND :endDate " +
                        "GROUP BY a.status")
        List<Object[]> getAppointmentCountByStatus(LocalDate startDate, LocalDate endDate);
}
