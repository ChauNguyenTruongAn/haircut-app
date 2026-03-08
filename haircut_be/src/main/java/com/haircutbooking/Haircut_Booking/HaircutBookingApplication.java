package com.haircutbooking.Haircut_Booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class HaircutBookingApplication {

	public static void main(String[] args) {
		SpringApplication.run(HaircutBookingApplication.class, args);
	}

}
