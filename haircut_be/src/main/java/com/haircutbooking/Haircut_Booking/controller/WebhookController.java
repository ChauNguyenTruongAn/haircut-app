package com.haircutbooking.Haircut_Booking.controller;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.messenger4j.Messenger;
import com.haircutbooking.Haircut_Booking.domain.ChatSession;
import com.haircutbooking.Haircut_Booking.domain.webhook.WebhookPayload;
import com.haircutbooking.Haircut_Booking.services.ChatService;
import com.haircutbooking.Haircut_Booking.services.DialogflowService;
import com.haircutbooking.Haircut_Booking.services.MessengerService;
import com.haircutbooking.Haircut_Booking.services.HaircutService;
import com.haircutbooking.Haircut_Booking.services.BookingService;
import com.haircutbooking.Haircut_Booking.dto.TimeSlotDTO;
import java.lang.StringBuilder;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);
    private final ChatService chatService;
    private final DialogflowService dialogflowService;
    private final HaircutService haircutService;
    private final ObjectMapper objectMapper;
    private final BookingService bookingService;

    @Value("${SPRING_VERIFY_TOKEN}")
    private String verifyToken;

    @Value("${DIALOGFLOW_WEBHOOK_URL}")
    private String dialogflowWebhookUrl;

    @Value("${dialogflow.project-id}")
    private String dialogflowProjectId;

    @Value("${SPRING_VERIFY_TOKEN}")
    private String facebookVerifyToken;

    @GetMapping
    public ResponseEntity<String> verifyWebhook(@RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String verifyToken,
            @RequestParam("hub.challenge") String challenge) {
        logger.info("Verifying webhook: mode={}, verify_token={}", mode, verifyToken);

        if ("subscribe".equals(mode) && facebookVerifyToken.equals(verifyToken)) {
            logger.info("Webhook verified successfully");
            return ResponseEntity.ok(challenge);
        } else {
            logger.error("Webhook verification failed");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Handle webhook from Facebook
     */
    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestParam(value = "hub.mode", required = false) String mode,
            @RequestParam(value = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(value = "hub.challenge", required = false) String challenge,
            @RequestBody(required = false) String rawPayload) {

        // Verification endpoint for Facebook webhook setup
        if (mode != null && verifyToken != null && challenge != null) {
            return verifyWebhook(mode, verifyToken, challenge);
        }

        // Handle webhook payload
        if (rawPayload != null && !rawPayload.isEmpty()) {
            // Parse JSON payload
            try {
                WebhookPayload payload = objectMapper.readValue(rawPayload, WebhookPayload.class);
                logger.debug("Received webhook payload: {}", payload);

                if (payload.getObject() == null) {
                    return ResponseEntity.badRequest().build();
                }

                if ("page".equalsIgnoreCase(payload.getObject())) {
                    // Đây là webhook từ Facebook Page, xử lý các sự kiện
                    if (payload.getEntry() != null && !payload.getEntry().isEmpty()) {
                        for (WebhookPayload.Entry entry : payload.getEntry()) {
                            if (entry.getMessaging() != null && !entry.getMessaging().isEmpty()) {
                                // Xử lý các sự kiện tin nhắn từ Facebook Messenger
                                for (WebhookPayload.MessagingEvent event : entry.getMessaging()) {
                                    handleMessagingEvent(event, rawPayload);
                                }
                            }
                        }
                    }
                }

                return ResponseEntity.ok("EVENT_RECEIVED");
            } catch (Exception e) {
                logger.error("Error processing webhook: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }

        return ResponseEntity.badRequest().build();
    }

    /**
     * Handle a messaging event from Facebook Messenger
     */
    private void handleMessagingEvent(WebhookPayload.MessagingEvent event, String rawPayload) {
        try {
            if (event.getSender() == null) {
                logger.error("Invalid messaging event: missing sender");
                return;
            }

            String senderId = event.getSender().getId();
            logger.info("Processing messaging event from sender ID: {}", senderId);

            // Check if this is a message
            if (event.getMessage() == null) {
                logger.info("Received non-message event from sender {}", senderId);
                return;
            }

            // Get or create a chat session for this user
            String sessionId = dialogflowService.createNewSessionId();
            ChatSession session = chatService.getOrCreateChatSession(
                    senderId, "FACEBOOK", sessionId);

            // Extract message text
            String messageText = event.getMessage().getText();
            if (messageText == null || messageText.isEmpty()) {
                logger.info("Received empty message or non-text message (perhaps an attachment)");
                // Facebook tự xử lý tin nhắn trống hoặc attachment
                // Không cần gửi tin nhắn trực tiếp từ server
                return;
            }

            // Record user message in our database
            chatService.recordUserMessage(session, messageText, null, rawPayload);

            // Facebook sẽ tự động chuyển tin nhắn này tới Dialogflow qua tích hợp Facebook
            // Messenger
            // Không cần gọi API của Dialogflow từ đây
            // Chỉ cần lưu phiên làm việc và context parameters để sử dụng sau
            session.getContext().put("SENDER_ID", senderId);
            session.getContext().put("MESSAGE_TEXT", messageText);
            session.setLastInteraction(LocalDateTime.now());
            chatService.updateSessionContext(session, session.getContext());

            logger.info("Message recorded and session updated for sender: {}", senderId);
            // Dialogflow sẽ trả lại phản hồi thông qua webhook /webhook/dialogflow
        } catch (Exception e) {
            logger.error("Error processing messaging event: ", e);
        }
    }

}
