package com.haircutbooking.Haircut_Booking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.huggingface.HuggingFaceChatModel;

@Configuration
public class LlmConfig {

    @Value("${SPRING_HUGGING_ACCESS_TOKEN}")
    private String huggingFaceToken;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public HuggingFaceService huggingFaceService() {
        return new HuggingFaceService(restTemplate(), huggingFaceToken);
    }

    public static class HuggingFaceService {
        private final RestTemplate restTemplate;
        private final String accessToken;

        public HuggingFaceService(RestTemplate restTemplate, String accessToken) {
            this.restTemplate = restTemplate;
            this.accessToken = accessToken;
        }

        public String generateText(String prompt) {
            return null;
        }
    }
}