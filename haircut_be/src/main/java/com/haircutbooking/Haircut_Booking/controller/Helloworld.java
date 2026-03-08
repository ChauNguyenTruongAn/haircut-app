package com.haircutbooking.Haircut_Booking.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/media")
public class Helloworld {

    @GetMapping
    public String getMedia() {
        return new String("Hello from An Châu");
    }

    @GetMapping("/auth")
    public String showHello() {
        return new String("An xin chào. Vui lòng gửi An 50k nhé");
    }

    @GetMapping("/auth/login")
    public String login() {
        return new String("Hello from login");
    }

}