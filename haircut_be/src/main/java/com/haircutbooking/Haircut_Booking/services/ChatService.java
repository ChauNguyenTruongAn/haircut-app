package com.haircutbooking.Haircut_Booking.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.haircutbooking.Haircut_Booking.domain.ChatMessage;
import com.haircutbooking.Haircut_Booking.domain.ChatMessage.MessageType;
import com.haircutbooking.Haircut_Booking.domain.ChatSession;
import com.haircutbooking.Haircut_Booking.domain.User;
import com.haircutbooking.Haircut_Booking.repositories.ChatMessageRepository;
import com.haircutbooking.Haircut_Booking.repositories.ChatSessionRepository;
import com.haircutbooking.Haircut_Booking.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    /**
     * Gets or creates a chat session for the specified external user ID and
     * platform
     */
    @Transactional
    public ChatSession getOrCreateChatSession(String externalUserId, String platform, String sessionId) {
        // Try to find active session for this user on this platform
        Optional<ChatSession> existingSession = chatSessionRepository
                .findByExternalUserIdAndPlatformAndActiveTrue(externalUserId, platform);

        if (existingSession.isPresent()) {
            ChatSession session = existingSession.get();
            session.setLastInteraction(LocalDateTime.now());
            return chatSessionRepository.save(session);
        }

        // Create new session
        ChatSession newSession = ChatSession.builder()
                .sessionId(sessionId)
                .externalUserId(externalUserId)
                .platform(platform)
                .lastInteraction(LocalDateTime.now())
                .active(true)
                .build();

        return chatSessionRepository.save(newSession);
    }

    /**
     * Records a message from a user
     */
    @Transactional
    public ChatMessage recordUserMessage(ChatSession session, String messageText, String intentName,
            String rawPayload) {
        ChatMessage message = ChatMessage.builder()
                .chatSession(session)
                .timestamp(LocalDateTime.now())
                .messageType(MessageType.USER)
                .messageText(messageText)
                .intentName(intentName)
                .rawPayload(rawPayload)
                .build();

        return chatMessageRepository.save(message);
    }

    /**
     * Records a bot response
     */
    @Transactional
    public ChatMessage recordBotMessage(ChatSession session, String messageText, String intentName, String rawPayload) {
        ChatMessage message = ChatMessage.builder()
                .chatSession(session)
                .timestamp(LocalDateTime.now())
                .messageType(MessageType.BOT)
                .messageText(messageText)
                .intentName(intentName)
                .rawPayload(rawPayload)
                .build();

        return chatMessageRepository.save(message);
    }

    /**
     * Updates context parameters for a session
     */
    @Transactional
    public void updateSessionContext(ChatSession session, Map<String, String> parameters) {
        session.getContext().putAll(parameters);
        session.setLastInteraction(LocalDateTime.now());
        chatSessionRepository.save(session);
    }

    /**
     * Updates the intent for a session
     */
    @Transactional
    public void updateSessionIntent(ChatSession session, String intentName) {
        session.setCurrentIntent(intentName);
        session.setLastInteraction(LocalDateTime.now());
        chatSessionRepository.save(session);
    }

    /**
     * Links a chat session to a user account
     */
    @Transactional
    public boolean linkSessionToUser(ChatSession session, String phoneNumber) {
        Optional<User> user = userRepository.findByPhoneNumber(phoneNumber);

        if (user.isPresent()) {
            User foundUser = user.get();
            foundUser.addChatSession(session);
            userRepository.save(foundUser);
            return true;
        }

        return false;
    }

    /**
     * Gets conversation history for a session
     */
    public List<ChatMessage> getConversationHistory(String sessionId) {
        return chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
    }

    /**
     * Close a chat session
     */
    @Transactional
    public void closeSession(ChatSession session) {
        session.setActive(false);
        chatSessionRepository.save(session);
    }

    /**
     * Process Dialogflow payload and extract key information
     */
    public void processDialogflowPayload(JsonNode payload, ChatSession session) {
        try {
            // Extract the query text (what the user said)
            String queryText = payload.path("queryResult").path("queryText").asText("");

            // Extract the intent information
            String intentName = payload.path("queryResult").path("intent").path("displayName").asText("");

            // Extract parameters
            JsonNode parametersNode = payload.path("queryResult").path("parameters");

            // Update the session with intent and parameters
            updateSessionIntent(session, intentName);

            // Record the user message
            recordUserMessage(session, queryText, intentName, payload.toString());

            // Extract and save parameters from the payload
            if (!parametersNode.isMissingNode()) {
                parametersNode.fields().forEachRemaining(entry -> {
                    if (!entry.getValue().isNull() && !entry.getValue().asText().isEmpty()) {
                        session.getContext().put(entry.getKey(), entry.getValue().asText());
                    }
                });
                chatSessionRepository.save(session);
            }

        } catch (Exception e) {
            logger.error("Error processing Dialogflow payload: {}", e.getMessage());
        }
    }

    /**
     * Get active chat session by external user ID (e.g., Facebook sender ID)
     * 
     * @param externalUserId The external user ID
     * @return The chat session, or null if not found
     */
    public ChatSession getActiveChatSessionByExternalUserId(String externalUserId) {
        logger.info("Getting active chat session for external user ID: {}", externalUserId);
        return chatSessionRepository.findByExternalUserIdAndActiveTrue(externalUserId)
                .orElse(null);
    }

    public Map<String, String> getSessionContext(ChatSession session) {
        return session.getContext();
    }

    public ChatSession getOrCreateSession(String sessionId) {
        Optional<ChatSession> sessionOpt = chatSessionRepository.findBySessionId(sessionId);
        if (sessionOpt.isPresent()) {
            return sessionOpt.get();
        }

        ChatSession session = new ChatSession();
        session.setSessionId(sessionId);
        session.setContext(new HashMap<>());
        session.setLastInteraction(LocalDateTime.now());
        session.setActive(true);
        session.setPlatform("DIALOGFLOW");
        return chatSessionRepository.save(session);
    }

    public void saveSession(ChatSession session) {
        chatSessionRepository.save(session);
    }
}