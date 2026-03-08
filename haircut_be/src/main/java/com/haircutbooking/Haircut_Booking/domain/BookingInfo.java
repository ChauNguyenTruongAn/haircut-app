package com.haircutbooking.Haircut_Booking.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

public class BookingInfo {
    private String name;
    private String phone;
    private String service;
    private LocalDate date;
    private LocalTime time;
    private Map<String, String> context;

    public BookingInfo() {
        this.context = new HashMap<>();
    }

    public boolean isNameValid() {
        return name != null && !name.trim().isEmpty();
    }

    public boolean isPhoneValid() {
        return phone != null && phone.matches("^0[0-9]{9}$");
    }

    public boolean isServiceValid() {
        return service != null && !service.trim().isEmpty();
    }

    public boolean isDateTimeValid() {
        return date != null && time != null;
    }

    public boolean isComplete() {
        return isNameValid() && isPhoneValid() && isServiceValid() && isDateTimeValid();
    }

    public String getMissingInfo() {
        if (!isNameValid())
            return "name";
        if (!isPhoneValid())
            return "phone";
        if (!isServiceValid())
            return "service";
        if (!isDateTimeValid())
            return "datetime";
        return null;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public Map<String, String> getContext() {
        return context;
    }

    public void setContext(Map<String, String> context) {
        this.context = context;
    }
}