package com.haircutbooking.Haircut_Booking.domain.webhook;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    public String mid;
    public String text;
    public List<Command> commands;
}
