package com.haircutbooking.Haircut_Booking.config;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.springframework.beans.factory.annotation.Value;

import jakarta.servlet.http.HttpServletRequest;

public class VNPayConfig {

    @Value("${SPRING_BACKEND_URL}")
    private static String vnp_Url_Return;

    // Sandbox
    public static final String vnp_TmnCode = "0BPR8WNQ";
    public static final String vnp_HashSecret = "C7SRMVKQKTG4LPF7CM3ASQBM6HG21QS5";
    public static final String vnp_PayUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    public static final String vnp_ReturnUrl = vnp_Url_Return + "/api/vnpay/payment-callback";
    public static final String vnp_ApiUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";

    public static String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    public static String getTimeStamp() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT+7"));
        return formatter.format(new Date());
    }
}
