package com.haircutbooking.Haircut_Booking.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.haircutbooking.Haircut_Booking.services.MessengerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/messenger")
@RequiredArgsConstructor
public class MessengerController {

    private static final Logger logger = LoggerFactory.getLogger(MessengerController.class);
    private final MessengerService messengerService;

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody MessageRequest request) {
        logger.info("Received request to send message to recipient: {}", request.getRecipientId());

        boolean success = messengerService.sendTextMessage(request.getRecipientId(), request.getMessage());

        if (success) {
            return ResponseEntity.ok().body(new MessageResponse("Message sent successfully"));
        } else {
            return ResponseEntity.internalServerError().body(new MessageResponse("Failed to send message"));
        }
    }

    /**
     * Gửi tin nhắn cụ thể đến user có ID được chỉ định
     * Endpoint này mô phỏng yêu cầu trực tiếp đến Facebook Graph API
     */
    @PostMapping("/send/{recipientId}/{message}")
    public ResponseEntity<?> sendSpecificMessage(
            @PathVariable("recipientId") String recipientId,
            @PathVariable("message") String message) {

        logger.info("Sending specific message to recipient: {}", recipientId);

        boolean success = messengerService.sendTextMessage(recipientId, message);

        if (success) {
            return ResponseEntity.ok().body(new MessageResponse("Message sent successfully"));
        } else {
            return ResponseEntity.internalServerError().body(new MessageResponse("Failed to send message"));
        }
    }

    /**
     * Gửi tin nhắn theo định dạng chính xác như yêu cầu
     * POST https://graph.facebook.com/v21.0/me/messages
     * Body: {"message":{"text":"Nội dung tin
     * nhắn"},"recipient":{"id":"ID_người_nhận"}}
     * Header: Authorization Bearer [PAGE_TOKEN]
     */
    @PostMapping("/raw")
    public ResponseEntity<?> sendRawMessage(@RequestBody RawMessageRequest request) {
        logger.info("Sending raw message to recipient: {}", request.getRecipientId());

        boolean success = messengerService.sendRawMessage(request.getRecipientId(), request.getText());

        if (success) {
            return ResponseEntity.ok().body(new MessageResponse("Raw message sent successfully"));
        } else {
            return ResponseEntity.internalServerError().body(new MessageResponse("Failed to send raw message"));
        }
    }

    /**
     * DTO cho request gửi tin nhắn
     */
    public static class MessageRequest {
        private String recipientId;
        private String message;

        // Getters and setters
        public String getRecipientId() {
            return recipientId;
        }

        public void setRecipientId(String recipientId) {
            this.recipientId = recipientId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    /**
     * DTO cho request gửi tin nhắn raw format
     */
    public static class RawMessageRequest {
        private String recipientId;
        private String text;

        // Getters and setters
        public String getRecipientId() {
            return recipientId;
        }

        public void setRecipientId(String recipientId) {
            this.recipientId = recipientId;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    /**
     * DTO cho response sau khi gửi tin nhắn
     */
    public static class MessageResponse {
        private String status;

        public MessageResponse(String status) {
            this.status = status;
        }

        public String getStatus() {
            return status;
        }
    }
}