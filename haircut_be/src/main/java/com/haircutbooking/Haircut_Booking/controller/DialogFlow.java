package com.haircutbooking.Haircut_Booking.controller;

import java.time.LocalDateTime;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.haircutbooking.Haircut_Booking.domain.Appointment;
import com.haircutbooking.Haircut_Booking.domain.ChatSession;
import com.haircutbooking.Haircut_Booking.domain.HaircutOption;
import com.haircutbooking.Haircut_Booking.domain.Payment;
import com.haircutbooking.Haircut_Booking.services.BookingService;
import com.haircutbooking.Haircut_Booking.services.ChatService;
import com.haircutbooking.Haircut_Booking.services.HaircutService;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import com.haircutbooking.Haircut_Booking.services.IntentAnalysisService;
import com.haircutbooking.Haircut_Booking.services.MessengerService;
import com.haircutbooking.Haircut_Booking.dto.AvailabilityDTO;
import com.haircutbooking.Haircut_Booking.dto.ServiceDTO;
import com.haircutbooking.Haircut_Booking.dto.AppointmentDTO;
import com.haircutbooking.Haircut_Booking.repositories.PaymentRepository;
import com.haircutbooking.Haircut_Booking.util.PaymentStatus;
import com.haircutbooking.Haircut_Booking.util.VNPayUtil;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.Set;
import java.math.BigDecimal;
import com.haircutbooking.Haircut_Booking.exception.ResourceNotFoundException;
import com.haircutbooking.Haircut_Booking.repositories.HaircutOptionRepository;

@RestController
@RequiredArgsConstructor
@Slf4j
public class DialogFlow {
    private final ChatService chatService;
    private final BookingService bookingService;
    private final RestTemplate restTemplate;
    private final IntentAnalysisService intentAnalysisService;
    private final PaymentRepository paymentRepository;
    private final HaircutService haircutService;
    private final MessengerService messengerService;
    private final HaircutOptionRepository haircutOptionRepository;

    @Value("${SPRING_PAGE_TOKEN}")
    private String page_access_token;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @PostMapping("/dialogflow")
    public ResponseEntity<Map<String, Object>> handleDialogFlow(@RequestBody Map<String, Object> request) {
        // Extract important information from request
        Map<String, Object> queryResult = (Map<String, Object>) request.get("queryResult");
        String queryText = queryResult != null ? (String) queryResult.get("queryText") : null;

        // Get output contexts parameters
        List<Map<String, Object>> outputContexts = (List<Map<String, Object>>) queryResult.get("outputContexts");
        Map<String, Object> parameters = new HashMap<>();
        if (outputContexts != null && !outputContexts.isEmpty()) {
            for (Map<String, Object> context : outputContexts) {
                if (context.get("parameters") != null) {
                    parameters.putAll((Map<String, Object>) context.get("parameters"));
                }
            }
        }

        // Get sender ID from originalDetectIntentRequest
        Map<String, Object> originalRequest = (Map<String, Object>) request.get("originalDetectIntentRequest");
        String senderId = null;
        if (originalRequest != null) {
            Map<String, Object> payload = (Map<String, Object>) originalRequest.get("payload");
            if (payload != null && payload.get("data") != null) {
                Map<String, Object> data = (Map<String, Object>) payload.get("data");
                if (data.get("sender") != null) {
                    Map<String, Object> sender = (Map<String, Object>) data.get("sender");
                    senderId = (String) sender.get("id");
                }
            }
        }

        // Get intent display name
        Map<String, Object> intent = (Map<String, Object>) queryResult.get("intent");
        String displayName = intent != null ? (String) intent.get("displayName") : null;

        // Log extracted information
        log.info("Extracted Dialogflow information:");
        log.info("Query Text: {}\n", queryText);
        log.info("Parameters: {}\n", parameters);
        log.info("Sender ID: {}\n", senderId);
        log.info("Intent Display Name: {}\n", displayName);

        Map<String, Object> response = new HashMap<>();
        response = processIntent(displayName, senderId, parameters);
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> processIntent(String intent, String senderId, Map<String, Object> parameters) {
        switch (intent) {
            case "haircut.welcome":
                return handleWelcomeIntent(senderId);
            case "haircut.booking.service":
                return handleBookingServiceIntent(senderId, parameters);
            case "haircut.booking.date-time":
                return handleBookingDateTimeIntent(senderId, parameters);
            case "haircut.booking.info":
                return handleBookingInfoIntent(senderId, parameters);
            case "haircut.booking.confirm":
                return handleBookingConfirmIntent(senderId, parameters);
            case "haircut.booking.cancel":
                return handleBookingCancelIntent(senderId, parameters);
            case "haircut.price.inquiry":
                return handlePriceInquiryIntent(senderId, parameters);
            default:
                return sendMessage(List.of("Xin chào! Tôi đã nhận được tin nhắn của bạn.", "Đây là tin nhắn mặc định"));
        }
    }

    private Map<String, Object> handleWelcomeIntent(String senderId) {
        String message = """
                An Barber xin chào! Rất vui khi được bạn quan tâm. Bạn đang muốn cắt tóc, tạo kiểu hay cần tư vấn về dịch vụ nào ạ?
                An Barber sẵn sàng hỗ trợ để giúp bạn có trải nghiệm tốt nhất tại tiệm.
                """;
        messengerService.sendRawMessage(senderId, message);
        String availableServices = haircutService.getServicesDescription();
        return sendMessage(List.of(availableServices));
    }

    private Map<String, Object> handleBookingServiceIntent(String senderId, Map<String, Object> parameters) {
        Map<String, Object> response = new HashMap<>();
        List<ServiceDTO> systemServices = haircutService.getAllServices();
        List<String> matchedServices = new ArrayList<>();

        List<String> requestedServices = (List<String>) parameters.get("service");
        if (requestedServices != null) {
            for (String requestedService : requestedServices) {
                String requestedServiceLower = requestedService.toLowerCase();
                for (ServiceDTO systemService : systemServices) {
                    if (systemService.getName().toLowerCase().contains(requestedServiceLower) ||
                            requestedServiceLower.contains(systemService.getName().toLowerCase())) {
                        matchedServices.add(systemService.getName());
                        response.put(systemService.getName(), systemService.getDescription());
                    }
                }
            }
        }

        if (!matchedServices.isEmpty()) {
            String message = "";
            if (matchedServices.size() > 1) {
                message = "Bạn đã chọn các dịch vụ: " + String.join(", ", matchedServices);
            } else {
                message = "Bạn đã chọn dịch vụ: " + matchedServices.get(0).toString();
            }
            messengerService.sendRawMessage(senderId, message);
        } else {
            String message = "Xin lỗi, tôi không tìm thấy dịch vụ phù hợp với yêu cầu của bạn. Mời bạn chọn lại dịch vụ.";
            messengerService.sendRawMessage(senderId, haircutService.getServicesDescription());
            return sendMessage(List.of(message));
        }

        return sendMessage(List.of("Hãy cho mình xin thời gian bạn muốn đặt lịch nhé."));
    }

    private Map<String, Object> handleBookingDateTimeIntent(String senderId, Map<String, Object> parameters) {
        try {
            String dateTimeStr = (String) parameters.get("date-time");
            String timeStr = (String) parameters.get("time");

            // Parse date from date-time
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr.substring(0, 19));
            String date = dateTime.format(DATE_FORMATTER);

            // Parse time from time
            LocalDateTime time = LocalDateTime.parse(timeStr.substring(0, 19));
            String hour = time.format(TIME_FORMATTER);

            // Kiểm tra thời gian
            LocalTime bookingTime = time.toLocalTime();
            LocalDate bookingDate = dateTime.toLocalDate();

            // Kiểm tra thời gian không được trong quá khứ
            if (LocalDateTime.of(bookingDate, bookingTime).isBefore(LocalDateTime.now())) {
                return sendMessage(List.of("Không thể đặt lịch trong quá khứ. Vui lòng chọn thời gian khác."));
            }

            // Kiểm tra thời gian không được quá xa (1 tháng)
            if (bookingDate.isAfter(LocalDate.now().plusMonths(1))) {
                return sendMessage(
                        List.of("Chỉ có thể đặt lịch trong vòng 1 tháng tới. Vui lòng chọn thời gian khác."));
            }

            // Kiểm tra giờ làm việc (8h-20h)
            if (bookingTime.isBefore(LocalTime.of(8, 0)) || bookingTime.isAfter(LocalTime.of(20, 0))) {
                return sendMessage(List.of("Giờ làm việc từ 8h đến 20h. Vui lòng chọn thời gian khác."));
            }

            // Kiểm tra thời gian phải là bội số của 30 phút
            if (bookingTime.getMinute() % 30 != 0) {
                return sendMessage(List.of("Vui lòng chọn thời gian là bội số của 30 phút (ví dụ: 8:00, 8:30, 9:00)."));
            }

            // Kiểm tra dịch vụ
            List<String> requestedServices = (List<String>) parameters.get("service");
            if (requestedServices == null || requestedServices.isEmpty()) {
                return sendMessage(List.of("Vui lòng chọn dịch vụ trước khi chọn thời gian."));
            }

            List<ServiceDTO> systemServices = haircutService.getAllServices();
            List<String> matchedServices = new ArrayList<>();
            Set<Long> serviceIds = new HashSet<>();

            for (String requestedService : requestedServices) {
                String requestedServiceLower = requestedService.toLowerCase();
                for (ServiceDTO systemService : systemServices) {
                    if (systemService.getName().toLowerCase().contains(requestedServiceLower) ||
                            requestedServiceLower.contains(systemService.getName().toLowerCase())) {
                        matchedServices.add(systemService.getName());
                        serviceIds.add(systemService.getId());
                    }
                }
            }

            if (matchedServices.isEmpty()) {
                return sendMessage(List.of("Không tìm thấy dịch vụ phù hợp. Vui lòng chọn lại dịch vụ."));
            }

            // Kiểm tra thời gian có trống không
            AvailabilityDTO availability = bookingService.checkAvailability(bookingDate, bookingTime,
                    serviceIds.iterator().next());

            if (!availability.getIsAvailable()) {
                // Nếu có thời gian trống tiếp theo
                if (availability.getNextAvailableTime() != null) {
                    String message = String.format("""
                            Rất tiếc, khung giờ %s vào ngày %s đã kín lịch.
                            Tuy nhiên, chúng tôi có thể phục vụ bạn vào lúc %s vào ngày %s.
                            Bạn có muốn đặt lịch vào thời gian này không?""",
                            bookingTime.format(TIME_FORMATTER),
                            bookingDate.format(DATE_FORMATTER),
                            availability.getNextAvailableTime().format(TIME_FORMATTER),
                            availability.getDate().format(DATE_FORMATTER));
                    return sendMessage(List.of(message));
                } else {
                    String message = String.format("""
                            Rất tiếc, ngày %s đã kín lịch.
                            Vui lòng chọn ngày khác hoặc liên hệ với chúng tôi để được tư vấn thêm.""",
                            bookingDate.format(DATE_FORMATTER));
                    return sendMessage(List.of(message));
                }
            }

            // Nếu có thể đặt lịch, hiển thị thông tin chi tiết
            String message = String.format("""
                    Chúng tôi có thể phục vụ bạn vào lúc %s ngày %s.
                    Hiện có %d thợ cắt tóc rảnh.
                    Hãy cho mình xin tên và số điện thoại để hoàn tất việc đặt lịch nhé.""",
                    bookingTime.format(TIME_FORMATTER),
                    bookingDate.format(DATE_FORMATTER),
                    availability.getBarbersAvailable());
            return sendMessage(List.of(message));
        } catch (Exception e) {
            log.error("Error processing booking date time intent: ", e);
            return sendMessage(List.of("Xin lỗi, có lỗi xảy ra khi xử lý thời gian. Vui lòng thử lại."));
        }
    }

    private Map<String, Object> handleBookingInfoIntent(String senderId, Map<String, Object> parameters) {

        String dateTimeStr = (String) parameters.get("date-time");
        String timeStr = (String) parameters.get("time");

        // Parse date from date-time
        LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr.substring(0, 19));
        String date = dateTime.format(DATE_FORMATTER);

        // Parse time from time
        LocalDateTime time = LocalDateTime.parse(timeStr.substring(0, 19));
        String hour = time.format(TIME_FORMATTER);

        List<ServiceDTO> systemServices = haircutService.getAllServices();
        List<String> matchedServices = new ArrayList<>();

        Map<String, Object> response = new HashMap<>();

        List<String> requestedServices = (List<String>) parameters.get("service");
        if (requestedServices != null) {
            for (String requestedService : requestedServices) {
                String requestedServiceLower = requestedService.toLowerCase();
                for (ServiceDTO systemService : systemServices) {
                    if (systemService.getName().toLowerCase().contains(requestedServiceLower) ||
                            requestedServiceLower.contains(systemService.getName().toLowerCase())) {
                        matchedServices.add(systemService.getName());
                        response.put(systemService.getName(), systemService.getDescription());
                    }
                }
            }
        }

        String phone = (String) parameters.get("phone-number");

        String message = "Thông tin đặt lịch của bạn:\n" +
                "- Dịch vụ: " + String.join(", ", matchedServices) + "\n" +
                "- Ngày: " + date + "\n" +
                "- Giờ: " + hour + "\n" +
                "- Số điện thoại: " + phone + "\n\n" +
                "Bạn có đồng ý đặt lịch với thông tin trên không?";

        return sendMessage(List.of(message));
    }

    private Map<String, Object> handleBookingConfirmIntent(String senderId, Map<String, Object> parameters) {
        try {
            String dateTimeStr = (String) parameters.get("date-time");
            String timeStr = (String) parameters.get("time");

            // Parse date from date-time
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr.substring(0, 19));
            String date = dateTime.format(DATE_FORMATTER);

            // Parse time from time
            LocalDateTime time = LocalDateTime.parse(timeStr.substring(0, 19));
            String hour = time.format(TIME_FORMATTER);

            List<ServiceDTO> systemServices = haircutService.getAllServices();
            List<String> matchedServices = new ArrayList<>();
            Set<Long> serviceIds = new HashSet<>();

            List<String> requestedServices = (List<String>) parameters.get("service");
            if (requestedServices != null) {
                for (String requestedService : requestedServices) {
                    String requestedServiceLower = requestedService.toLowerCase();
                    for (ServiceDTO systemService : systemServices) {
                        if (systemService.getName().toLowerCase().contains(requestedServiceLower)
                                || requestedServiceLower.contains(systemService.getName().toLowerCase())) {
                            matchedServices.add(systemService.getName());
                            serviceIds.add(systemService.getId());
                        }
                    }
                }
            }

            String phone = (String) parameters.get("phone-number");
            Map<String, Object> person = (Map<String, Object>) parameters.get("person");
            String name = (String) person.get("name");

            // Lấy thông tin từ context
            String servicesStr = String.join(", ", matchedServices);
            String customerName = name;
            String customerPhone = phone;

            // Kiểm tra thông tin bắt buộc
            if (date == null || time == null || servicesStr == null || customerPhone == null) {
                return sendMessage(List.of("Thiếu thông tin cần thiết để đặt lịch. Vui lòng thử lại."));
            }

            // Parse thời gian
            LocalDate bookingDate = dateTime.toLocalDate();
            LocalTime bookingTime = time.toLocalTime();

            if (serviceIds.isEmpty()) {
                return sendMessage(List.of("Không tìm thấy dịch vụ. Vui lòng thử lại."));
            }

            // Tạo appointment DTO
            AppointmentDTO appointmentDTO = AppointmentDTO.builder()
                    .date(bookingDate)
                    .startTime(bookingTime)
                    .customerName(customerName != null ? customerName : "Khách hàng " + customerPhone)
                    .customerPhone(customerPhone)
                    .serviceIds(serviceIds)
                    .build();

            // Tạo appointment
            Appointment appointment = bookingService.createAppointment(appointmentDTO);

            if (appointment != null) {
                // Tính tổng giá của tất cả dịch vụ
                BigDecimal totalPrice = BigDecimal.ZERO;
                List<String> serviceNames = new ArrayList<>();
                for (Long serviceId : serviceIds) {
                    HaircutOption service = haircutOptionRepository.findById(serviceId)
                            .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
                    totalPrice = totalPrice.add(service.getBasePrice());
                    serviceNames.add(service.getName());
                }

                DecimalFormat decimalFormat = new DecimalFormat("0.#");
                String amount = decimalFormat.format(totalPrice);

                // Tạo URL thanh toán VNPay
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/vnpay/create-payment?amount=" + amount))
                        .header("accept", "*/*")
                        .GET()
                        .build();

                HttpResponse<String> reponseLinkVnpay = client.send(request, HttpResponse.BodyHandlers.ofString());
                String vnpayUrl = reponseLinkVnpay.body();
                String txnRef = VNPayUtil.getTxnRef(vnpayUrl);

                // Tạo payment record
                Payment payment = new Payment();
                payment.setAppointment(appointment);
                payment.setAmount(totalPrice.longValue());
                payment.setPaymentMethod("vnpay");
                payment.setStatus(PaymentStatus.PENDING);
                payment.setTransactionReference(txnRef);
                paymentRepository.save(payment);

                // Gửi thông báo xác nhận

                // Gửi nút thanh toán

                // Tạo thông báo chi tiết
                StringBuilder message = new StringBuilder("Thông tin lịch hẹn:\n");
                message.append("- Tên: ").append(appointmentDTO.getCustomerName()).append("\n");
                message.append("- Số điện thoại: ").append(customerPhone).append("\n");
                message.append("- Dịch vụ: ").append(String.join(", ", serviceNames)).append("\n");
                message.append("- Tổng tiền: ").append(amount).append("đ\n");
                message.append("- Thợ cắt tóc: ").append(appointment.getBarber().getName()).append("\n");
                message.append("- Ngày: ").append(bookingDate.format(DATE_FORMATTER)).append("\n");
                message.append("- Giờ: ").append(bookingTime.format(TIME_FORMATTER)).append("\n");
                message.append("- Mã đặt lịch: ").append(appointment.getId()).append("\n");
                // Tạo nội dung thông báo
                sendMessage(List.of(message.toString()));

                String confirmationMessage = "🎉 Đặt lịch thành công!\n" +
                        "Mã đặt lịch: *" + appointment.getId() + "*\n" +
                        "Tổng tiền: " + amount + "đ\n" +
                        "💳 Vui lòng nhấn nút bên dưới để thanh toán.";
                messengerService.sendRawMessage(senderId, confirmationMessage);
                sendPaymentMessage(senderId, vnpayUrl, serviceNames, amount);

                String thankYouMessage = """
                        Hẹn gặp mình vào %s ngày %s.\nCó gì cần hỗ trợ thêm mình cứ nhắn em liền nha!
                        Em cảm ơn mình đã đặt lịch tại tiệm nha 🧡\n
                            """;
                return sendMessage(List.of(thankYouMessage.formatted(bookingTime.format(TIME_FORMATTER),
                        bookingDate.format(DATE_FORMATTER))));
            }
            return sendMessage(List.of("Không thể tạo lịch hẹn. Vui lòng thử lại sau."));
        } catch (Exception e) {
            log.error("Error processing booking confirmation: ", e);
            return sendMessage(List.of("Có lỗi xảy ra khi đặt lịch. Nhân viên tư vấn sẽ sớm trả lời bạn."));
        }
    }

    private Map<String, Object> handleBookingCancelIntent(String senderId, Map<String, Object> parameters) {
        try {
            // Lấy giá trị từ parameters và chuyển đổi sang Long
            Object bookingIdObj = parameters.get("number_appointment");
            Long bookingId;

            if (bookingIdObj instanceof Double) {
                bookingId = ((Double) bookingIdObj).longValue();
            } else if (bookingIdObj instanceof Integer) {
                bookingId = ((Integer) bookingIdObj).longValue();
            } else if (bookingIdObj instanceof String) {
                bookingId = Long.parseLong((String) bookingIdObj);
            } else {
                return sendMessage(List.of("Mã đặt lịch không hợp lệ. Vui lòng thử lại."));
            }

            // Hủy đơn
            boolean cancelled = bookingService.cancelAppointment(bookingId);

            String responseMessage = "";
            if (cancelled) {
                responseMessage = """
                        An Barber đã hủy lịch hẹn thành công cho mình nhé.
                        Rất mong được phục vụ bạn vào dịp khác. Nếu cần đặt lịch lại, cứ nhắn em bất kỳ lúc nào ạ.
                        Chúc bạn một ngày thật vui và nhiều năng lượng! 🌿✂️""";
            } else {
                responseMessage = """
                        Ôi, An Barber xin lỗi vì hiện tại chưa thể hủy lịch như yêu cầu của mình được ạ.
                        An Barber đang kiểm tra lại giúp mình, chút xíu nhân viên sẽ báo ngay nha. Cảm ơn mình đã kiên nhẫn chờ chút xíu nha 🧡""";
            }
            return sendMessage(List.of(responseMessage));
        } catch (Exception e) {
            log.error("Error processing booking cancel intent: ", e);
            return sendMessage(List.of("Nhân viên tư vấn sẽ sớm trả lời bạn. Mong bạn thông cảm. 🕵️"));
        }
    }

    private Map<String, Object> handleDefaultIntent(ChatSession session, String userMessage) {
        String availableServices = haircutService.getServicesDescription();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Xin lỗi, tôi không hiểu yêu cầu của bạn. " +
                "Bạn có thể cho biết bạn muốn đặt lịch cắt tóc không?\n" +
                availableServices);
        response.put("status", "error");
        return response;
    }

    private void sendPaymentMessage(String recipientId, String paymentUrl, List<String> serviceNames, String amount) {
        String url = "https://graph.facebook.com/v19.0/me/messages?access_token=" + page_access_token;

        // Tạo message body với template có nút thanh toán
        Map<String, Object> message = new HashMap<>();
        message.put("recipient", Map.of("id", recipientId));
        message.put("message", Map.of(
                "attachment", Map.of(
                        "type", "template",
                        "payload", Map.of(
                                "template_type", "generic",
                                "elements", List.of(
                                        Map.of(
                                                "title", "Dịch vụ: " + String.join(", ", serviceNames),
                                                "subtitle", "Giá: " + amount + "đ\nNhấn nút bên dưới để thanh toán",
                                                "image_url",
                                                "https://scontent.fsgn17-1.fna.fbcdn.net/v/t39.30808-1/487298653_2069008643605780_2335830044472999046_n.jpg?stp=c0.0.1367.1367a_dst-jpg_s200x200_tt6&_nc_cat=107&ccb=1-7&_nc_sid=2d3e12&_nc_eui2=AeHnzbeZfNZ_oEGgJ-DDv32qh5yaYvSCfxmHnJpi9IJ_GYjFcVM1o7KfBC6CmL_dla4VB-RhBexMe8TZWGUiC5Os&_nc_ohc=-XV-TND3ri0Q7kNvwFiZN3j&_nc_oc=AdnXmLvAXmGD1tNBwSrinlTjZXYKD_Zhd0o1n2669ZwHQFrZEQI7C4xcmrIX2tXr7Ugwuabr8cZ1LElETkqjxI2Y&_nc_zt=24&_nc_ht=scontent.fsgn17-1.fna&_nc_gid=vZ8BHeClxtEEH9jvK02L7g&oh=00_AfETM2XYAZKW5mnULaFxOeC6gO5-tnw7jX52yXsKAJtwTQ&oe=68071269",
                                                "buttons", List.of(
                                                        Map.of(
                                                                "type", "web_url",
                                                                "url", paymentUrl,
                                                                "title", "Thanh toán ngay",
                                                                "webview_height_ratio", "tall"))))))));

        // Gửi request đến Facebook Messenger API
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(message, headers);
        restTemplate.postForEntity(url, entity, String.class);
    }

    private Map<String, Object> sendMessage(List<String> message) {
        // Create a simple Dialogflow response structure
        Map<String, Object> response = new HashMap<>();

        // Create fulfillment messages
        Map<String, Object> textMessage = new HashMap<>();
        message.stream().forEach(m -> textMessage.put("text", List.of(m)));

        Map<String, Object> fulfillmentMessage = new HashMap<>();
        fulfillmentMessage.put("text", textMessage);

        response.put("fulfillmentMessages", List.of(fulfillmentMessage));
        response.put("fulfillmentText", "Xin chào! Tôi đã nhận được tin nhắn của bạn ở dưới.");
        response.put("source", "haircut-booking");
        return response;
    }

    private Map<String, Object> handlePriceInquiryIntent(String senderId, Map<String, Object> parameters) {
        try {
            List<ServiceDTO> systemServices = haircutService.getAllServices();
            List<String> matchedServices = new ArrayList<>();
            List<String> serviceDetails = new ArrayList<>();

            List<String> requestedServices = (List<String>) parameters.get("service");
            if (requestedServices != null) {
                for (String requestedService : requestedServices) {
                    String requestedServiceLower = requestedService.toLowerCase();
                    for (ServiceDTO systemService : systemServices) {
                        if (systemService.getName().toLowerCase().contains(requestedServiceLower)
                                || requestedServiceLower.contains(systemService.getName().toLowerCase())) {
                            matchedServices.add(systemService.getName());
                            // Thêm thông tin chi tiết về dịch vụ
                            String detail = String.format("- %s: %s\n  Giá: %s đồng",
                                    systemService.getName(),
                                    systemService.getDescription(),
                                    systemService.getBasePrice());
                            serviceDetails.add(detail);
                        }
                    }
                }
            }

            if (matchedServices.isEmpty()) {
                return sendMessage(List.of(
                        "Xin lỗi, tôi không tìm thấy dịch vụ phù hợp. Mời bạn xem danh sách dịch vụ của chúng tôi:"));
            }

            // Tạo message chi tiết
            StringBuilder message = new StringBuilder("Thông tin về dịch vụ bạn quan tâm:\n\n");
            for (String detail : serviceDetails) {
                message.append(detail).append("\n\n");
            }
            message.append("Bạn có thể đặt lịch ngay bây giờ hoặc liên hệ với chúng tôi để được tư vấn thêm.");

            return sendMessage(List.of(message.toString()));
        } catch (Exception e) {
            log.error("Error processing price inquiry: ", e);
            return sendMessage(List.of("Xin lỗi, có lỗi xảy ra khi tra cứu giá dịch vụ. Vui lòng thử lại sau."));
        }
    }
}
