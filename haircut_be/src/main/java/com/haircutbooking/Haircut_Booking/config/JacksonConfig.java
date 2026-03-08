package com.haircutbooking.Haircut_Booking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class JacksonConfig {
    // @Bean
    // public ObjectMapper objectMapper() {
    //     ObjectMapper mapper = new ObjectMapper();
    //     mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    //     mapper.enable(SerializationFeature.INDENT_OUTPUT);
    //     mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    //     mapper.registerModule(new JavaTimeModule());
    //     return mapper;
    // }
}
