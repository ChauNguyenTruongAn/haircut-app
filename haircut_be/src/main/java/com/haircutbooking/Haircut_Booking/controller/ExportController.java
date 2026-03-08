package com.haircutbooking.Haircut_Booking.controller;

import com.haircutbooking.Haircut_Booking.dto.AppointmentDTO;
import com.haircutbooking.Haircut_Booking.domain.Appointment;
import com.haircutbooking.Haircut_Booking.domain.HaircutOption;
import com.haircutbooking.Haircut_Booking.domain.ResponseDTO.AppointmentResponse;
import com.haircutbooking.Haircut_Booking.services.AppointmentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/export")
public class ExportController {
    private final AppointmentService appointmentService;

    public ExportController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/appointments")
    public ResponseEntity<byte[]> exportAppointments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<AppointmentDTO> appointments;
            if (startDate != null && endDate != null) {
                appointments = appointmentService.getAppointmentsByDateRange(startDate, endDate)
                        .stream()
                        .map(this::convertToAppointmentDTO)
                        .collect(Collectors.toList());
            } else {
                appointments = appointmentService.getAllAppointments()
                        .stream()
                        .map(this::convertFromResponse)
                        .collect(Collectors.toList());
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            // Export logic here
            outputStream.close();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "appointments.xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(outputStream.toByteArray());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private AppointmentDTO convertToAppointmentDTO(Appointment appointment) {
        return AppointmentDTO.builder()
                .id(appointment.getId())
                .userId(appointment.getCustomer() != null ? appointment.getCustomer().getId() : null)
                .barberId(appointment.getBarber().getId())
                .barberName(appointment.getBarber().getName())
                .date(appointment.getDate())
                .startTime(appointment.getStartTime())
                .endTime(appointment.getEndTime())
                .customerName(appointment.getCustomerName())
                .customerPhone(appointment.getCustomerPhone())
                .notes(appointment.getNotes())
                .totalPrice(appointment.getTotalPrice().doubleValue())
                .services(new ArrayList<>(appointment.getServices()))
                .status(appointment.getStatus().name())
                .build();
    }

    private AppointmentDTO convertFromResponse(AppointmentResponse response) {
        return AppointmentDTO.builder()
                .userId(response.getUserId())
                .barberId(response.getBarberId())
                .date(response.getAppointmentDate())
                .startTime(response.getAppointmentTime())
                .endTime(response.getAppointmentTimeEnd())
                .status(response.getStatus())
                .notes(response.getNote())
                .services(new ArrayList<>(response.getServices()))
                .build();
    }
}