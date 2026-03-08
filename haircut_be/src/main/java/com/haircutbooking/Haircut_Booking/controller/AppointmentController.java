package com.haircutbooking.Haircut_Booking.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.haircutbooking.Haircut_Booking.domain.RequestDTO.AddAppointmentRequest;
import com.haircutbooking.Haircut_Booking.domain.RequestDTO.UpdateAppointmentStatusRequest;
import com.haircutbooking.Haircut_Booking.domain.ResponseDTO.AppointmentResponse;
import com.haircutbooking.Haircut_Booking.exception.ResourceNotFoundException;
import com.haircutbooking.Haircut_Booking.services.AppointmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointment", description = "Appointment management APIs")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @Operation(summary = "Create a new appointment", description = "Creates a new appointment with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Appointment created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppointmentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input provided", content = @Content(mediaType = "application/json"))
    })
    @PostMapping
    public ResponseEntity<?> createAppointment(
            @Parameter(description = "Appointment request object", required = true) @Valid @RequestBody AddAppointmentRequest request) {
        try {
            AppointmentResponse appointment = appointmentService.createAppointment(request);
            return ResponseEntity.ok(appointment);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @Operation(summary = "Get all appointments", description = "Returns a list of all appointments")
    @ApiResponse(responseCode = "200", description = "Found all appointments", content = @Content(mediaType = "application/json"))
    @GetMapping
    public ResponseEntity<?> getAllAppointments() {
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }

    @Operation(summary = "Get appointment by ID", description = "Returns a single appointment by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the appointment", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppointmentResponse.class))),
            @ApiResponse(responseCode = "404", description = "Appointment not found", content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getAppointmentById(
            @Parameter(description = "ID of appointment to be searched", required = true) @PathVariable Long id) {
        try {
            AppointmentResponse appointment = appointmentService.getAppointmentById(id);
            return ResponseEntity.ok(appointment);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @Operation(summary = "Update an appointment", description = "Updates an appointment with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Appointment updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppointmentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input provided", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Appointment not found", content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAppointment(
            @Parameter(description = "ID of appointment to be updated", required = true) @PathVariable Long id,
            @Parameter(description = "Updated appointment details", required = true) @Valid @RequestBody AddAppointmentRequest request) {
        try {
            AppointmentResponse updatedAppointment = appointmentService.updateAppointment(id, request);
            return ResponseEntity.ok(updatedAppointment);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @Operation(summary = "Delete an appointment", description = "Deletes an appointment by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Appointment deleted successfully", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Appointment not found", content = @Content(mediaType = "application/json"))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAppointment(
            @Parameter(description = "ID of appointment to be deleted", required = true) @PathVariable Long id)
            throws Exception {
        appointmentService.deleteAppointment(id);
        return ResponseEntity.ok("Appointment deleted successfully");
    }

    @Operation(summary = "Get appointments by date", description = "Returns a list of appointments for a specific date")
    @ApiResponse(responseCode = "200", description = "Found appointments for the requested date", content = @Content(mediaType = "application/json"))
    @GetMapping("/date/{date}")
    public ResponseEntity<?> getAppointmentsByDate(
            @Parameter(description = "Date in format YYYY-MM-DD", required = true) @PathVariable String date) {
        try {
            LocalDate parsedDate = LocalDate.parse(date);
            return ResponseEntity.ok(appointmentService.getAppointmentsByDate(parsedDate));
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body("Invalid date format. Please use YYYY-MM-DD");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching appointments: " + e.getMessage());
        }
    }

    @Operation(summary = "Update appointment status", description = "Updates the status of an appointment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Appointment status updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppointmentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input provided", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Appointment not found", content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateAppointmentStatus(
            @Parameter(description = "ID of appointment to update status", required = true) @PathVariable Long id,
            @Parameter(description = "Status update request object", required = true) @Valid @RequestBody UpdateAppointmentStatusRequest request) {
        try {
            AppointmentResponse updatedAppointment = appointmentService.updateAppointmentStatus(id, request.getStatus(),
                    request.getReason());
            return ResponseEntity.ok(updatedAppointment);
        } catch (Exception e) {
            if (e instanceof ResourceNotFoundException) {
                return ResponseEntity.status(404).body(e.getMessage());
            }
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }
}