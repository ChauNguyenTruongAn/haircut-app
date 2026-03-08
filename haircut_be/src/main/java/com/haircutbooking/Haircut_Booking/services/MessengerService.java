package com.haircutbooking.Haircut_Booking.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class MessengerService {

    private static final Logger logger = LoggerFactory.getLogger(MessengerService.class);
    private static final String FACEBOOK_GRAPH_API_URL = "https://graph.facebook.com/v21.0/me/messages";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${SPRING_PAGE_TOKEN}")
    private String pageToken;

    /**
     * Gửi tin nhắn văn bản đến người dùng Facebook Messenger
     *
     * @param recipientId PSID của người nhận
     * @param message     Nội dung tin nhắn
     * @return true nếu gửi thành công, false nếu thất bại
     */
    public boolean sendTextMessage(String recipientId, String message) {
        try {
            logger.info("Sending message to recipient: {}", recipientId);

            // Chuẩn bị dữ liệu theo đúng định dạng Facebook Messenger API
            String payload = buildMessagePayload(recipientId, message);

            // Tạo HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(FACEBOOK_GRAPH_API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + pageToken)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            // Gửi request và lấy response
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Kiểm tra kết quả
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                logger.info("Message sent successfully. Response: {}", response.body());
                return true;
            } else {
                logger.error("Failed to send message. Status code: {}, Response: {}",
                        response.statusCode(), response.body());
                return false;
            }

        } catch (Exception e) {
            logger.error("Error sending message to Facebook: ", e);
            return false;
        }
    }

    /**
     * Gửi tin nhắn với định dạng chính xác theo yêu cầu API Facebook
     * Đây là phương thức gửi tin nhắn chính xác theo định dạng yêu cầu
     */
    public boolean sendRawMessage(String recipientId, String messageText) {
        try {
            logger.info("Sending raw message to recipient: {}", recipientId);

            // Tạo JSON theo định dạng chính xác từ yêu cầu
            String payload = String.format(
                    "{" +
                            "\"message\": {\"text\":\"%s\"}," +
                            "\"recipient\": {\"id\":\"%s\"}" +
                            "}",
                    escapeJson(messageText),
                    recipientId);

            // Tạo HTTP request với bearer token
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(FACEBOOK_GRAPH_API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + pageToken)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            // Gửi request và lấy response
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Kiểm tra kết quả
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                logger.info("Raw message sent successfully. Response: {}", response.body());
                return true;
            } else {
                logger.error("Failed to send raw message. Status code: {}, Response: {}",
                        response.statusCode(), response.body());
                return false;
            }

        } catch (Exception e) {
            logger.error("Error sending raw message to Facebook: ", e);
            return false;
        }
    }

    /**
     * Tạo JSON payload cho API gửi tin nhắn
     */
    private String buildMessagePayload(String recipientId, String textMessage) throws Exception {
        // Tạo JSON với định dạng mà Facebook yêu cầu
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{");
        jsonBuilder.append("\"recipient\":{\"id\":\"").append(recipientId).append("\"},");
        jsonBuilder.append("\"message\":{\"text\":\"").append(escapeJson(textMessage)).append("\"}");
        jsonBuilder.append("}");

        return jsonBuilder.toString();
    }

    /**
     * Escape các ký tự đặc biệt trong JSON
     */
    private String escapeJson(String input) {
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}