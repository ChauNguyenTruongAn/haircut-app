package com.haircutbooking.Haircut_Booking.services;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haircutbooking.Haircut_Booking.config.LlmConfig.HuggingFaceService;
import com.haircutbooking.Haircut_Booking.domain.Barber;
import com.haircutbooking.Haircut_Booking.domain.HaircutOption;
import com.haircutbooking.Haircut_Booking.domain.ResponseDTO.LlmIntentResponse;
import com.haircutbooking.Haircut_Booking.repositories.BarberRepository;
import com.haircutbooking.Haircut_Booking.repositories.HaircutOptionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmIntentService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final BarberRepository barberRepository;
    private final HaircutOptionRepository haircutOptionRepository;

    @Value("${SPRING_HUGGING_ACCESS_TOKEN}")
    private String apiKey;

    /**
     * Phân tích ý định từ tin nhắn của người dùng khi Dialogflow không nhận diện
     * được
     */
    public LlmIntentResponse analyzeIntent(String userMessage) {
        try {
            // Tạo system prompt để hướng dẫn mô hình
            String systemPrompt = "Bạn là trợ lý AI cho tiệm tóc. "
                    + "Hãy phân tích tin nhắn của khách hàng và xác định ý định đặt lịch cắt tóc. "
                    + "Trả về kết quả dưới dạng JSON theo cấu trúc sau:\n"
                    + "{\n"
                    + "  \"intent\": \"booking_appointment | check_schedule | cancel_appointment | other\",\n"
                    + "  \"service\": {\n"
                    + "    \"name\": \"tên dịch vụ (cắt tóc, uốn, nhuộm...)\",\n"
                    + "    \"type\": \"loại dịch vụ nếu có\"\n"
                    + "  },\n"
                    + "  \"time\": {\n"
                    + "    \"date\": \"ngày đặt lịch nếu có (format dd/MM/yyyy)\",\n"
                    + "    \"time\": \"giờ đặt lịch nếu có (format HH:mm)\"\n"
                    + "  }\n"
                    + "}\n"
                    + "Chỉ trả về JSON, không thêm bất kỳ giải thích hay văn bản nào khác.";

            // Gọi HuggingFace API trực tiếp
            String result = callHuggingFaceAPI(systemPrompt, userMessage);

            // Tìm vị trí bắt đầu và kết thúc của JSON
            int startIndex = result.indexOf("{");
            int endIndex = result.lastIndexOf("}") + 1;

            if (startIndex >= 0 && endIndex > startIndex) {
                String jsonResult = result.substring(startIndex, endIndex);
                log.info("LLM response JSON: {}", jsonResult);

                // Parse JSON thành object
                return objectMapper.readValue(jsonResult, LlmIntentResponse.class);
            } else {
                log.error("Could not find valid JSON in the response: {}", result);
                return new LlmIntentResponse();
            }
        } catch (Exception e) {
            log.error("Error when analyzing intent with LLM: ", e);
            return new LlmIntentResponse();
        }
    }

    /**
     * Phân tích ý định với context từ dịch vụ và thợ cắt tóc hiện có
     */
    public LlmIntentResponse analyzeIntentWithContext(String userMessage) {
        try {
            // Lấy dữ liệu context từ database
            List<HaircutOption> services = haircutOptionRepository.findByIsActiveTrueOrderByNameAsc();
            List<Barber> barbers = barberRepository.findByIsActiveTrueOrderByNameAsc();

            // Tạo context về dịch vụ và barber
            String servicesContext = services.stream()
                    .map(s -> s.getName() + " (" + s.getDurationMinutes() + " phút, " + s.getBasePrice() + "đ)")
                    .collect(Collectors.joining(", "));

            String barbersContext = barbers.stream()
                    .map(b -> b.getName() + " (Giờ làm việc: " + b.getStartWorkingHour() + " - " + b.getEndWorkingHour()
                            + ")")
                    .collect(Collectors.joining(", "));

            // Tạo prompt với context
            String contextPrompt = "Bạn là trợ lý AI cho tiệm tóc có các dịch vụ sau: " + servicesContext + ". " +
                    "Các thợ cắt tóc hiện có: " + barbersContext + ". " +
                    "Hãy phân tích tin nhắn của khách hàng: \"" + userMessage + "\" " +
                    "Trả về JSON theo cấu trúc sau:\n" +
                    "{\n" +
                    "  \"intent\": \"booking_appointment | check_schedule | cancel_appointment | other\",\n" +
                    "  \"service\": {\n" +
                    "    \"name\": \"tên dịch vụ (cắt tóc, uốn, nhuộm...)\",\n" +
                    "    \"type\": \"loại dịch vụ nếu có\"\n" +
                    "  },\n" +
                    "  \"time\": {\n" +
                    "    \"date\": \"ngày đặt lịch nếu có (format dd/MM/yyyy)\",\n" +
                    "    \"time\": \"giờ đặt lịch nếu có (format HH:mm)\"\n" +
                    "  }\n" +
                    "}\n" +
                    "Chỉ trả về JSON, không thêm bất kỳ giải thích hay văn bản nào khác.";

            // Gọi API
            String result = callHuggingFaceAPI(contextPrompt, userMessage);

            // Xử lý response tương tự như phương thức trước
                int startIndex = result.indexOf("{");
            int endIndex = result.lastIndexOf("}") + 1;

            if (startIndex >= 0 && endIndex > startIndex) {
                String jsonResult = result.substring(startIndex, endIndex);
                log.info("LLM with context response JSON: {}", jsonResult);

                return objectMapper.readValue(jsonResult, LlmIntentResponse.class);
            } else {
                log.error("Could not find valid JSON in the response with context: {}", result);
                return new LlmIntentResponse();
            }
        } catch (Exception e) {
            log.error("Error analyzing intent with context: ", e);
            return new LlmIntentResponse();
        }
    }

    /**
     * Gọi HuggingFace API trực tiếp
     */
    public String callHuggingFaceAPI(String systemPrompt, String userMessage) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = new HashMap<>();

            // Tạo messages array
            List<Map<String, Object>> messages = new ArrayList<>();

            // System message
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);

            // User message
            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            requestBody.put("messages", messages);
            requestBody.put("max_tokens", 512);
            requestBody.put("model", "meta-llama/Llama-4-Scout-17B-16E-Instruct");
            requestBody.put("stream", false);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("=== LLM API REQUEST ===");
            log.info("User message: {}", userMessage);
            log.info("System prompt preview: {}",
                    systemPrompt.substring(0, Math.min(100, systemPrompt.length())) + "...");

            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://router.huggingface.co/together/v1/chat/completions",
                    HttpMethod.POST,
                    entity,
                    Map.class);

            log.info("HuggingFace API response status: {}", response.getStatusCode());

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                log.error("Empty response from HuggingFace API");
                return "";
            }

            // Log usage information if available
            if (responseBody.containsKey("usage")) {
                Map<String, Object> usage = (Map<String, Object>) responseBody.get("usage");
                log.info("LLM usage - Prompt tokens: {}, Completion tokens: {}, Total tokens: {}",
                        usage.get("prompt_tokens"), usage.get("completion_tokens"), usage.get("total_tokens"));
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices == null || choices.isEmpty()) {
                log.error("No choices in response from HuggingFace API");
                return "";
            }

            Map<String, Object> firstChoice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
            if (message == null) {
                log.error("No message in response from HuggingFace API");
                return "";
            }

            String content = (String) message.get("content");
            if (content == null) {
                log.error("No content in response from HuggingFace API");
                return "";
            }

            log.info("=== LLM API RESPONSE ===");
            log.info("Raw response text: {}", content);

            return content;

        } catch (Exception e) {
            log.error("Error calling HuggingFace API: ", e);
            log.error("Stack trace: ", e);
            return "";
        }
    }
}