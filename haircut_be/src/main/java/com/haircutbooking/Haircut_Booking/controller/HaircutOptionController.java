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

import com.haircutbooking.Haircut_Booking.domain.RequestDTO.AddServiceRequest;
import com.haircutbooking.Haircut_Booking.services.HaircutService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/services")
@RequiredArgsConstructor
public class HaircutOptionController {
    private final HaircutService serviceEntityService;

    @GetMapping
    public ResponseEntity<?> getAllServices() {
        return ResponseEntity.ok(serviceEntityService.getAllServices());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getServiceById(@PathVariable Long id) throws Exception {
        return ResponseEntity.ok(serviceEntityService.getServiceById(id));
    }

    @PostMapping
    public ResponseEntity<?> addService(@RequestBody AddServiceRequest request) {
        return ResponseEntity.ok(serviceEntityService.addService(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editService(@PathVariable Long id, @RequestBody AddServiceRequest request)
            throws Exception {
        return ResponseEntity.ok(serviceEntityService.editService(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteService(@PathVariable Long id) {
        serviceEntityService.deleteService(id);
        return ResponseEntity.ok("Service removed successfully");
    }
}