package com.haircutbooking.Haircut_Booking.domain.webhook;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageValue {
    public Sender sender;
    public Recipient recipient;
    public String timestamp;
    public Message message;
}