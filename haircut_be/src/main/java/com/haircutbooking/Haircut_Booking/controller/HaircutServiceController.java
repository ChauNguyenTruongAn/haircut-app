package com.haircutbooking.Haircut_Booking.controller;

import java.util.List;
import java.util.Optional;

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

import com.haircutbooking.Haircut_Booking.domain.HaircutOption;
import com.haircutbooking.Haircut_Booking.services.HaircutServiceManager;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class HaircutServiceController {

    private final HaircutServiceManager serviceManager;

    @GetMapping
    public ResponseEntity<List<HaircutOption>> getAllServices() {
        return ResponseEntity.ok(serviceManager.getAllActiveServices());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HaircutOption> getServiceById(@PathVariable Long id) {
        Optional<HaircutOption> service = serviceManager.getServiceById(id);
        return service.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<HaircutOption>> searchServices(@RequestParam String keyword) {
        return ResponseEntity.ok(serviceManager.searchServices(keyword));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<HaircutOption> createService(@RequestBody HaircutOption service) {
        return ResponseEntity.ok(serviceManager.saveService(service));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<HaircutOption> updateService(@PathVariable Long id, @RequestBody HaircutOption service) {
        service.setId(id);
        return ResponseEntity.ok(serviceManager.saveService(service));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateService(@PathVariable Long id) {
        serviceManager.deactivateService(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/barber/{barberId}")
    public ResponseEntity<List<HaircutOption>> getServicesByBarber(@PathVariable Long barberId) {
        return ResponseEntity.ok(serviceManager.getServicesByBarber(barberId));
    }
}