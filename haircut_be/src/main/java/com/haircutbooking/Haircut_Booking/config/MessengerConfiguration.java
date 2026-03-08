package com.haircutbooking.Haircut_Booking.config;

import com.github.messenger4j.Messenger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessengerConfiguration {

    @Value("${SPRING_SECRET_KEY}")
    private String appSecret;

    @Value("${SPRING_PAGE_TOKEN}")
    private String pageAccessToken;

    @Value("${SPRING_VERIFY_TOKEN}")
    private String verifyToken;

    @Bean
    public Messenger messenger() {
        return Messenger.create(appSecret, pageAccessToken, verifyToken);
    }
}
