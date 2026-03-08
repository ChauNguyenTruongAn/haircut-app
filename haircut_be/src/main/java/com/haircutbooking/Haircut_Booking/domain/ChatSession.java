package com.haircutbooking.Haircut_Booking.domain;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chat_sessions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession extends AbstractEntity {

    @Column(name = "session_id", nullable = false, unique = true)
    private String sessionId;

    @Column(name = "platform", nullable = false)
    private String platform; // MESSENGER, DIALOGFLOW, etc

    @Column(name = "external_user_id")
    private String externalUserId; // Messenger/Dialogflow user ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonBackReference
    private User user; // Can be null if user is not authenticated

    @Column(name = "last_interaction", nullable = false)
    private LocalDateTime lastInteraction;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "current_intent")
    private String currentIntent;

    @ElementCollection
    @CollectionTable(name = "chat_session_context", joinColumns = @JoinColumn(name = "chat_session_id"))
    @MapKeyColumn(name = "context_key")
    @Column(name = "context_value")
    private Map<String, String> context = new HashMap<>();

    @Column(name = "pending_appointment_id")
    private Long pendingAppointmentId; // ID of appointment being created in this session

    private String lastMessage;

    public Map<String, String> getContext() {
        return context;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }
}