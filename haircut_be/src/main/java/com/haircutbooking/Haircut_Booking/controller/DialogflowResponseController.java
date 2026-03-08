package com.haircutbooking.Haircut_Booking.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haircutbooking.Haircut_Booking.services.DialogflowResponseService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/dialogflow/response")
@RequiredArgsConstructor
public class DialogflowResponseController {

    private static final Logger logger = LoggerFactory.getLogger(DialogflowResponseController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final DialogflowResponseService dialogflowResponseService;

    /**
     * Endpoint để xử lý response từ Dialogflow và gửi lại cho người dùng qua
     * Messenger
     */
    @PostMapping("/process")
    public ResponseEntity<?> processDialogflowResponse(@RequestBody DialogflowResponseRequest request) {
        logger.info("Received Dialogflow response processing request for recipient: {}", request.getRecipientId());

        try {
            boolean success = dialogflowResponseService.processDialogflowResponse(
                    request.getRecipientId(),
                    request.getResponseText());

            if (success) {
                return ResponseEntity.ok(new ProcessingResponse("Dialogflow response processed and sent successfully"));
            } else {
                return ResponseEntity.internalServerError()
                        .body(new ProcessingResponse("Failed to process Dialogflow response"));
            }
        } catch (Exception e) {
            logger.error("Error processing Dialogflow response", e);
            return ResponseEntity.internalServerError()
                    .body(new ProcessingResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Endpoint để parse và phân tích response từ Dialogflow
     */
    @PostMapping("/parse")
    public ResponseEntity<?> parseDialogflowResponse(@RequestBody String responseText) {
        logger.info("Received request to parse Dialogflow response");

        try {
            JsonNode responseJson = dialogflowResponseService.parseDialogflowResponse(responseText);
            if (responseJson == null) {
                return ResponseEntity.badRequest()
                        .body(new ProcessingResponse("Failed to parse Dialogflow response"));
            }

            // Trích xuất thông tin quan trọng
            String intentName = dialogflowResponseService.extractIntentName(responseJson);
            Map<String, Object> parameters = dialogflowResponseService.extractParameters(responseJson);
            String fulfillmentText = dialogflowResponseService.extractFulfillmentText(responseJson);
            String sessionId = dialogflowResponseService.extractSessionId(responseJson);

            // Tạo response object với thông tin đã trích xuất
            DialogflowParsedResponse parsedResponse = new DialogflowParsedResponse(
                    intentName,
                    parameters,
                    fulfillmentText,
                    sessionId);

            return ResponseEntity.ok(parsedResponse);
        } catch (Exception e) {
            logger.error("Error parsing Dialogflow response", e);
            return ResponseEntity.internalServerError()
                    .body(new ProcessingResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * DTO cho request xử lý response từ Dialogflow
     */
    public static class DialogflowResponseRequest {
        private String recipientId;
        private String responseText;

        // Getters and setters
        public String getRecipientId() {
            return recipientId;
        }

        public void setRecipientId(String recipientId) {
            this.recipientId = recipientId;
        }

        public String getResponseText() {
            return responseText;
        }

        public void setResponseText(String responseText) {
            this.responseText = responseText;
        }
    }

    /**
     * DTO cho response đã được parse từ Dialogflow
     */
    public static class DialogflowParsedResponse {
        private String intentName;
        private Map<String, Object> parameters;
        private String fulfillmentText;
        private String sessionId;

        public DialogflowParsedResponse(String intentName, Map<String, Object> parameters,
                String fulfillmentText, String sessionId) {
            this.intentName = intentName;
            this.parameters = parameters;
            this.fulfillmentText = fulfillmentText;
            this.sessionId = sessionId;
        }

        // Getters
        public String getIntentName() {
            return intentName;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public String getFulfillmentText() {
            return fulfillmentText;
        }

        public String getSessionId() {
            return sessionId;
        }
    }

    /**
     * DTO cho response thông báo kết quả xử lý
     */
    public static class ProcessingResponse {
        private String message;

        public ProcessingResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}