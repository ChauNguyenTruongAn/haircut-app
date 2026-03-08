package com.haircutbooking.Haircut_Booking.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.haircutbooking.Haircut_Booking.domain.User;
import com.haircutbooking.Haircut_Booking.domain.RequestDTO.ChangePasswordRequest;
import com.haircutbooking.Haircut_Booking.domain.RequestDTO.ModifyUserRequest;
import com.haircutbooking.Haircut_Booking.services.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PutMapping("/info/{id}")
    public ResponseEntity<?> modifyInfoUser(@PathVariable Long id, @RequestBody ModifyUserRequest request) {
        User user = userService.modifyUser(id, request);
        if (user != null) {
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User not found");
    }

    @PutMapping("/password/{id}")
    public ResponseEntity<?> putMethodName(@PathVariable String id, @RequestBody ChangePasswordRequest request) {
        User user = userService.changePasswordUser(id, request);
        if (user != null) {
            ResponseEntity.ok(user);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User not found");
    }
}
