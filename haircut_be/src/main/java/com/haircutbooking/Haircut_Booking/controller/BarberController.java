package com.haircutbooking.Haircut_Booking.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.haircutbooking.Haircut_Booking.domain.Barber;
import com.haircutbooking.Haircut_Booking.domain.Appointment;
import com.haircutbooking.Haircut_Booking.domain.HaircutOption;
import com.haircutbooking.Haircut_Booking.dto.TimeSlotDTO;
import com.haircutbooking.Haircut_Booking.services.BarberService;
import com.haircutbooking.Haircut_Booking.services.BookingService;
import com.haircutbooking.Haircut_Booking.services.HaircutServiceManager;
import com.haircutbooking.Haircut_Booking.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/barbers")
@RequiredArgsConstructor
public class BarberController {

    private final BarberService barberService;
    private final BookingService bookingService;
    private final HaircutServiceManager haircutServiceManager;

    @GetMapping
    public ResponseEntity<List<Barber>> getAllBarbers() {
        return ResponseEntity.ok(barberService.getAllActiveBarbers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Barber> getBarberById(@PathVariable Long id) {
        return barberService.getBarberById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/available")
    public ResponseEntity<List<Barber>> getAvailableBarbers() {
        return ResponseEntity.ok(barberService.getAllAvailableBarbers());
    }

    @GetMapping("/service/{serviceId}")
    public ResponseEntity<List<Barber>> getBarbersByService(@PathVariable Long serviceId) {
        return ResponseEntity.ok(barberService.getBarbersByService(serviceId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Barber> createBarber(@RequestBody Barber barber) {
        return ResponseEntity.ok(barberService.saveBarber(barber));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Barber> updateBarber(@PathVariable Long id, @RequestBody Map<String, Object> barberData) {
        try {
            // Get existing barber
            Barber existingBarber = barberService.getBarberById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Barber not found with id: " + id));

            // Update fields from request
            if (barberData.containsKey("name")) {
                existingBarber.setName((String) barberData.get("name"));
            }

            if (barberData.containsKey("email")) {
                existingBarber.setEmail((String) barberData.get("email"));
            }

            if (barberData.containsKey("phone")) {
                existingBarber.setPhone((String) barberData.get("phone"));
            }

            if (barberData.containsKey("position")) {
                existingBarber.setPosition((String) barberData.get("position"));
            }

            if (barberData.containsKey("bio")) {
                existingBarber.setBio((String) barberData.get("bio"));
            }

            if (barberData.containsKey("startWorkingHour")) {
                String timeString = (String) barberData.get("startWorkingHour");
                existingBarber.setStartWorkingHour(LocalTime.parse(timeString));
            }

            if (barberData.containsKey("endWorkingHour")) {
                String timeString = (String) barberData.get("endWorkingHour");
                existingBarber.setEndWorkingHour(LocalTime.parse(timeString));
            }

            if (barberData.containsKey("isActive")) {
                existingBarber.setIsActive((Boolean) barberData.get("isActive"));
            }

            if (barberData.containsKey("isAvailableForBooking")) {
                existingBarber.setIsAvailableForBooking((Boolean) barberData.get("isAvailableForBooking"));
            }

            if (barberData.containsKey("serviceIds")) {
                try {
                    // Handle service IDs update
                    @SuppressWarnings("unchecked")
                    List<Integer> serviceIds = (List<Integer>) barberData.get("serviceIds");
                    // Convert serviceIds to Set<HaircutOption>
                    Set<HaircutOption> serviceSet = new HashSet<>();

                    for (Integer serviceId : serviceIds) {
                        haircutServiceManager.getServiceById(serviceId.longValue())
                                .ifPresent(serviceSet::add);
                    }

                    existingBarber.setServices(serviceSet);
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException("Invalid format for serviceIds. Expected a list of integers.",
                            e);
                }
            }

            // Save updated barber
            return ResponseEntity.ok(barberService.saveBarber(existingBarber));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/appointments")
    public ResponseEntity<List<Appointment>> getBarberAppointments(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(bookingService.getBarberAppointments(id, date));
    }

    @DeleteMapping("/{id}")
    private ResponseEntity<?> delete(@PathVariable Long id) {
        barberService.deactivateBarber(id);
        return ResponseEntity.ok("Remove success");
    }
}
