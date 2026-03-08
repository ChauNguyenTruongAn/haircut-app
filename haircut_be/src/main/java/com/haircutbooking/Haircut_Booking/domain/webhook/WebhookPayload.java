package com.haircutbooking.Haircut_Booking.domain.webhook;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookPayload {
    // Basic fields common to all Facebook webhooks
    private String object;
    private List<Entry> entry;

    // The processed field and value for our application logic
    // (these are extracted from entry)
    private String field;
    private MessageValue value;

    // Nested class to represent an entry in the webhook
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entry {
        private String id;
        private Long time;

        // Messaging field for messenger
        @JsonProperty("messaging")
        private List<MessagingEvent> messaging;

        // Changes field for feed, etc.
        @JsonProperty("changes")
        private List<Change> changes;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MessagingEvent {
        private Sender sender;
        private Recipient recipient;
        private Long timestamp;
        private Message message;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Change {
        private String field;
        private MessageValue value;
    }
}
