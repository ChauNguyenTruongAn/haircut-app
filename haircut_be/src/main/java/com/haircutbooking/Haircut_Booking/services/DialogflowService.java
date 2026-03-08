package com.haircutbooking.Haircut_Booking.services;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DialogflowService {

    private static final Logger logger = LoggerFactory.getLogger(DialogflowService.class);

    private final ObjectMapper objectMapper;

    @Value("${dialogflow.project-id}")
    private String projectId;

    /**
     * Xử lý dữ liệu từ webhook Dialogflow
     * 
     * @param webhookRequestBody JSON payload từ Dialogflow webhook
     * @return Object chứa thông tin trích xuất từ webhook
     */
    public DialogflowResponse processWebhookRequest(String webhookRequestBody) {
        try {
            JsonNode requestNode = objectMapper.readTree(webhookRequestBody);

            // Trích xuất thông tin từ webhook request
            String queryText = requestNode.path("queryResult").path("queryText").asText("");
            String fulfillmentText = requestNode.path("queryResult").path("fulfillmentText").asText("");
            String intentName = requestNode.path("queryResult").path("intent").path("displayName").asText("");

            // Trích xuất parameters
            Map<String, String> parameters = new HashMap<>();
            JsonNode paramsNode = requestNode.path("queryResult").path("parameters");
            if (!paramsNode.isMissingNode()) {
                paramsNode.fields().forEachRemaining(entry -> {
                    if (!entry.getValue().isNull() && !entry.getValue().asText().isEmpty()) {
                        parameters.put(entry.getKey(), entry.getValue().asText());
                    }
                });
            }

            // Trích xuất thông tin người gửi từ Facebook
            String senderId = "";
            JsonNode originalDetectIntentRequest = requestNode.path("originalDetectIntentRequest");
            if (!originalDetectIntentRequest.isMissingNode() &&
                    originalDetectIntentRequest.path("source").asText().equals("facebook")) {
                senderId = originalDetectIntentRequest.path("payload")
                        .path("data")
                        .path("sender")
                        .path("id").asText("");
            }

            logger.info("Processed intent: {} from sender: {}", intentName, senderId);

            return new DialogflowResponse(fulfillmentText, intentName, parameters, requestNode);
        } catch (Exception e) {
            logger.error("Error processing Dialogflow webhook request: {}", e.getMessage());
            return new DialogflowResponse(
                    "Xin lỗi, tôi gặp sự cố khi xử lý yêu cầu của bạn. Vui lòng thử lại sau.",
                    "Error",
                    new HashMap<>(),
                    null);
        }
    }

    /**
     * Create a new session ID for Dialogflow
     */
    public String createNewSessionId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Response object from Dialogflow
     */
    public static class DialogflowResponse {
        private final String fulfillmentText;
        private final String intentName;
        private final Map<String, String> parameters;
        private final JsonNode rawResponse;

        public DialogflowResponse(String fulfillmentText, String intentName, Map<String, String> parameters,
                JsonNode rawResponse) {
            this.fulfillmentText = fulfillmentText;
            this.intentName = intentName;
            this.parameters = parameters;
            this.rawResponse = rawResponse;
        }

        public String getFulfillmentText() {
            return fulfillmentText;
        }

        public String getIntentName() {
            return intentName;
        }

        public Map<String, String> getParameters() {
            return parameters;
        }

        public JsonNode getRawResponse() {
            return rawResponse;
        }
    }
}