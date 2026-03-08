package com.haircutbooking.Haircut_Booking.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.haircutbooking.Haircut_Booking.domain.Appointment;
import com.haircutbooking.Haircut_Booking.dto.AppointmentDTO;
import com.haircutbooking.Haircut_Booking.dto.AvailabilityDTO;
import com.haircutbooking.Haircut_Booking.services.BookingService;
import com.haircutbooking.Haircut_Booking.util.AppointmentStatus;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private static final Logger log = LoggerFactory.getLogger(BookingController.class);

    @GetMapping("/check-availability")
    public ResponseEntity<AvailabilityDTO> checkAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time,
            @RequestParam Long serviceId) {
        return ResponseEntity.ok(bookingService.checkAvailability(date, time, serviceId));
    }

    @PostMapping
    public ResponseEntity<Appointment> createAppointment(@RequestBody AppointmentDTO appointmentDTO) {
        return ResponseEntity.ok(bookingService.createAppointment(appointmentDTO));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @PutMapping("/{id}/confirm")
    public ResponseEntity<Appointment> confirmAppointment(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.confirmAppointment(id));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Appointment> cancelAppointment(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(bookingService.cancelAppointment(id, reason));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @GetMapping("/date-range")
    public ResponseEntity<List<Appointment>> getAppointmentsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(bookingService.getAppointmentsByDateRange(startDate, endDate));
    }

    @GetMapping("/phone/{phone}")
    public ResponseEntity<List<Appointment>> findAppointmentsByPhone(@PathVariable String phone) {
        return ResponseEntity.ok(bookingService.findAppointmentsByPhone(phone));
    }

    @GetMapping("/appointments/status/{status}")
    public ResponseEntity<List<Appointment>> getAppointmentsByStatus(@PathVariable String status) {
        try {
            AppointmentStatus appointmentStatus = AppointmentStatus.valueOf(status.toUpperCase());
            List<Appointment> appointments = bookingService.getAppointmentsByStatus(appointmentStatus);
            return ResponseEntity.ok(appointments);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error getting appointments by status: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/revenue")
    public ResponseEntity<Map<String, Object>> getRevenue(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().withDayOfMonth(1);
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();

            // Lấy tổng doanh thu
            Double totalRevenue = bookingService.getTotalRevenue(start, end);

            // Lấy doanh thu theo dịch vụ
            Map<String, Double> revenueByService = bookingService.getRevenueByService(start, end);

            // Lấy số lượng đơn theo trạng thái
            Map<AppointmentStatus, Long> appointmentCountByStatus = bookingService.getAppointmentCountByStatus(start,
                    end);

            Map<String, Object> response = new HashMap<>();
            response.put("totalRevenue", totalRevenue);
            response.put("revenueByService", revenueByService);
            response.put("appointmentCountByStatus", appointmentCountByStatus);
            response.put("period", Map.of(
                    "startDate", start,
                    "endDate", end));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting revenue: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/total-revenue")
    public ResponseEntity<Map<String, Object>> getTotalRevenue(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().withDayOfMonth(1);
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();

            // Lấy tổng doanh thu
            Double totalRevenue = bookingService.getTotalRevenue(start, end);

            Map<String, Object> response = new HashMap<>();
            response.put("totalRevenue", totalRevenue);
            response.put("period", Map.of(
                    "startDate", start,
                    "endDate", end));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting total revenue: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}