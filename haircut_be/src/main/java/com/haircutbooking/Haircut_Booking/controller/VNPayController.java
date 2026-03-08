package com.haircutbooking.Haircut_Booking.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.UnsupportedEncodingException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import com.haircutbooking.Haircut_Booking.config.VNPayConfig;
import com.haircutbooking.Haircut_Booking.domain.Payment;
import com.haircutbooking.Haircut_Booking.repositories.PaymentRepository;
import com.haircutbooking.Haircut_Booking.util.PaymentStatus;
import com.haircutbooking.Haircut_Booking.util.AppointmentStatus;
import com.haircutbooking.Haircut_Booking.services.AppointmentService;
import com.haircutbooking.Haircut_Booking.services.MessengerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import jakarta.annotation.PostConstruct;

import com.haircutbooking.Haircut_Booking.domain.Appointment;
import com.haircutbooking.Haircut_Booking.repositories.AppointmentRepository;
import com.haircutbooking.Haircut_Booking.domain.User;

@RestController
@Tag(name = "VNPay Controller")
@RequiredArgsConstructor
@Slf4j
public class VNPayController {

    @Value("${SPRING_BACKEND_URL}")
    private String url_backend;

    @Value("${SPRING_URL_FRONTEND}")
    private String url_frontend;

    private String return_url;

    private final PaymentRepository paymentRepository;
    private final AppointmentService appointmentService;
    private final AppointmentRepository appointmentRepository;
    private final MessengerService messengerService;

    @PostConstruct
    public void init() {
        this.return_url = url_backend + "/api/vnpay/payment-callback";
    }

    @Operation(method = "GET", summary = "Payment Callback", description = "Xử lý callback của VNPay và chuyển hướng về trang frontend tương ứng với kết quả thanh toán")
    @GetMapping("/api/vnpay/payment-callback")
    public RedirectView paymentCallback(@RequestParam Map<String, String> params) throws UnsupportedEncodingException {
        String vnp_ResponseCode = params.get("vnp_ResponseCode");
        String vnp_TxnRef = params.get("vnp_TxnRef");

        log.info("Received VNPay callback - TxnRef: {}, ResponseCode: {}", vnp_TxnRef, vnp_ResponseCode);

        String baseRedirectUrl = url_frontend;
        String encodedTxnRef = URLEncoder.encode(vnp_TxnRef != null ? vnp_TxnRef : "",
                StandardCharsets.UTF_8.toString());

        try {
            Payment payment = paymentRepository.findByTransactionReference(vnp_TxnRef)
                    .orElseThrow(() -> new RuntimeException("Payment not found with TxnRef: " + vnp_TxnRef));

            if ("00".equals(vnp_ResponseCode)) {
                payment.setStatus(PaymentStatus.COMPLETED);
                paymentRepository.save(payment);

                appointmentService.updateAppointmentStatus(
                        payment.getAppointment().getId(),
                        AppointmentStatus.CONFIRMED,
                        "Payment completed successfully");

                // Gửi tin nhắn xác nhận
                User customer = payment.getAppointment().getCustomer();
                if (customer != null && customer.getSenderId() != null) {
                    String message = String.format(
                            "Thanh toán thành công cho lịch hẹn ngày %s lúc %s. Cảm ơn bạn đã sử dụng dịch vụ!",
                            payment.getAppointment().getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                            payment.getAppointment().getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")));
                    messengerService.sendRawMessage(customer.getSenderId(), message);
                }

                return new RedirectView(baseRedirectUrl + "?status=success&txnRef=" + encodedTxnRef);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);

                return new RedirectView(baseRedirectUrl + "?status=failed&txnRef=" + encodedTxnRef);
            }
        } catch (Exception e) {
            log.error("Error processing payment callback: ", e);
            return new RedirectView(baseRedirectUrl + "?status=error");
        }
    }

    @Operation(method = "GET", summary = "Tạo hóa đơn", description = "Đưa vào cái số tiền và sẽ trả về một cái link đó là link thanh toán.")
    @GetMapping("/api/vnpay/create-payment")
    public String createPayment(HttpServletRequest request, @RequestParam("amount") Long amount) {
        try {
            String vnp_Version = "2.1.0";
            String vnp_Command = "pay";
            String orderType = "other";
            String vnp_TxnRef = String.valueOf(System.currentTimeMillis());
            String vnp_IpAddr = VNPayConfig.getIpAddress(request);
            String vnp_TmnCode = VNPayConfig.vnp_TmnCode;

            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", vnp_Version);
            vnp_Params.put("vnp_Command", vnp_Command);
            vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
            vnp_Params.put("vnp_Amount", String.valueOf(amount * 100));
            vnp_Params.put("vnp_CurrCode", "VND");
            vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
            vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang " + vnp_TxnRef);
            vnp_Params.put("vnp_OrderType", orderType);
            vnp_Params.put("vnp_Locale", "vn");
            vnp_Params.put("vnp_ReturnUrl", return_url);
            vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
            vnp_Params.put("vnp_CreateDate", VNPayConfig.getTimeStamp());

            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();

            for (String fieldName : fieldNames) {
                String value = vnp_Params.get(fieldName);
                if ((value != null) && (!value.isEmpty())) {
                    hashData.append(fieldName).append('=').append(URLEncoder.encode(value, StandardCharsets.US_ASCII))
                            .append('&');
                    query.append(fieldName).append('=').append(URLEncoder.encode(value, StandardCharsets.US_ASCII))
                            .append('&');
                }
            }

            String queryUrl = query.substring(0, query.length() - 1);
            String secureHash = hmacSHA512(VNPayConfig.vnp_HashSecret, hashData.substring(0, hashData.length() - 1));
            queryUrl += "&vnp_SecureHash=" + secureHash;
            String paymentUrl = VNPayConfig.vnp_PayUrl + "?" + queryUrl;

            return paymentUrl;
        } catch (Exception e) {
            e.printStackTrace();
            return "Lỗi khi tạo thanh toán!";
        }
    }

    public String hmacSHA512(String key, String data) {
        try {
            Mac hmacSHA512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmacSHA512.init(secretKey);
            byte[] hash = hmacSHA512.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo chữ ký HMAC-SHA512", e);
        }
    }
}
