package com.haircutbooking.Haircut_Booking.services;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DialogflowResponseService {
    private static final Logger logger = LoggerFactory.getLogger(DialogflowResponseService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final MessengerService messengerService;

    /**
     * Parse response từ Dialogflow thành JSON object
     * 
     * @param responseText Kết quả JSON string nhận từ Dialogflow
     * @return JsonNode đã parse, hoặc null nếu xảy ra lỗi
     */
    public JsonNode parseDialogflowResponse(String responseText) {
        try {
            return objectMapper.readTree(responseText);
        } catch (JsonProcessingException e) {
            logger.error("Error parsing Dialogflow response: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Trích xuất thông tin intent từ JSON response
     * 
     * @param responseJson JsonNode đã parse từ Dialogflow
     * @return Tên của intent, hoặc null nếu không tìm thấy
     */
    public String extractIntentName(JsonNode responseJson) {
        if (responseJson == null)
            return null;

        try {
            return responseJson
                    .path("queryResult")
                    .path("intent")
                    .path("displayName")
                    .asText(null);
        } catch (Exception e) {
            logger.error("Error extracting intent name: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Trích xuất thông tin parameters từ JSON response
     * 
     * @param responseJson JsonNode đã parse từ Dialogflow
     * @return Map các parameters, hoặc null nếu không tìm thấy
     */
    public Map<String, Object> extractParameters(JsonNode responseJson) {
        if (responseJson == null)
            return null;

        try {
            JsonNode parametersNode = responseJson
                    .path("queryResult")
                    .path("parameters");

            if (parametersNode.isMissingNode() || parametersNode.isNull()) {
                return null;
            }

            return objectMapper.convertValue(parametersNode, Map.class);
        } catch (Exception e) {
            logger.error("Error extracting parameters: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Trích xuất thông tin tin nhắn phản hồi (fulfillmentText) từ JSON response
     * 
     * @param responseJson JsonNode đã parse từ Dialogflow
     * @return Text phản hồi, hoặc null nếu không tìm thấy
     */
    public String extractFulfillmentText(JsonNode responseJson) {
        if (responseJson == null)
            return null;

        try {
            return responseJson
                    .path("queryResult")
                    .path("fulfillmentText")
                    .asText(null);
        } catch (Exception e) {
            logger.error("Error extracting fulfillment text: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Trích xuất tất cả thông tin tin nhắn phản hồi từ JSON response
     * 
     * @param responseJson JsonNode đã parse từ Dialogflow
     * @return List các tin nhắn phản hồi, hoặc null nếu không tìm thấy
     */
    public List<String> extractFulfillmentMessages(JsonNode responseJson) {
        if (responseJson == null)
            return null;

        try {
            JsonNode messagesNode = responseJson
                    .path("queryResult")
                    .path("fulfillmentMessages");

            if (messagesNode.isMissingNode() || messagesNode.isNull() || !messagesNode.isArray()) {
                return null;
            }

            return objectMapper.convertValue(messagesNode, List.class);
        } catch (Exception e) {
            logger.error("Error extracting fulfillment messages: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Trích xuất session ID từ JSON response
     * 
     * @param responseJson JsonNode đã parse từ Dialogflow
     * @return Session ID, hoặc null nếu không tìm thấy
     */
    public String extractSessionId(JsonNode responseJson) {
        if (responseJson == null)
            return null;

        try {
            String fullSessionPath = responseJson.path("session").asText("");
            // Session path format: projects/project-id/agent/sessions/session-id
            String[] parts = fullSessionPath.split("/");
            return parts[parts.length - 1];
        } catch (Exception e) {
            logger.error("Error extracting session ID: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Gửi phản hồi từ Dialogflow đến người dùng thông qua Messenger
     * 
     * @param recipientId  ID người dùng trên Facebook
     * @param responseJson Kết quả từ Dialogflow đã parse
     * @return true nếu gửi thành công, false nếu thất bại
     */
    public boolean sendDialogflowResponseToUser(String recipientId, JsonNode responseJson) {
        if (responseJson == null)
            return false;

        String fulfillmentText = extractFulfillmentText(responseJson);
        if (fulfillmentText == null || fulfillmentText.isEmpty()) {
            logger.error("No fulfillment text found in Dialogflow response");
            return false;
        }

        return messengerService.sendTextMessage(recipientId, fulfillmentText);
    }

    /**
     * Xử lý toàn bộ quá trình: parse JSON, trích xuất thông tin, và gửi phản hồi
     * 
     * @param recipientId            ID người dùng trên Facebook
     * @param dialogflowResponseText Kết quả JSON string từ Dialogflow
     * @return true nếu toàn bộ quá trình thành công, false nếu thất bại
     */
    public boolean processDialogflowResponse(String recipientId, String dialogflowResponseText) {
        logger.info("Processing Dialogflow response for recipient: {}", recipientId);

        JsonNode responseJson = parseDialogflowResponse(dialogflowResponseText);
        if (responseJson == null) {
            logger.error("Failed to parse Dialogflow response");
            return false;
        }

        String intentName = extractIntentName(responseJson);
        logger.info("Detected intent: {}", intentName);

        Map<String, Object> parameters = extractParameters(responseJson);
        if (parameters != null) {
            logger.info("Extracted parameters: {}", parameters);
        }

        return sendDialogflowResponseToUser(recipientId, responseJson);
    }
}