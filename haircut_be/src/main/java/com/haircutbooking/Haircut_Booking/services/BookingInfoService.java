package com.haircutbooking.Haircut_Booking.services;

import com.haircutbooking.Haircut_Booking.domain.BookingInfo;
import com.haircutbooking.Haircut_Booking.domain.ChatSession;
import com.haircutbooking.Haircut_Booking.domain.ResponseDTO.LlmIntentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingInfoService {
    private final LlmIntentService llmIntentService;
    private final ChatService chatService;
    private final HaircutService haircutService;
    private final IntentAnalysisService intentAnalysisService;

    public Map<String, Object> processBookingInfo(String userMessage, ChatSession session) {
        Map<String, Object> response = new HashMap<>();

        // Lấy thông tin đặt lịch từ session
        BookingInfo bookingInfo = getBookingInfoFromSession(session);

        // Sử dụng LLM để phân tích tin nhắn của người dùng
        LlmIntentResponse llmResponse = llmIntentService.analyzeIntentWithContext(userMessage);

        // Cập nhật thông tin từ LLM response
        updateBookingInfoFromLlmResponse(bookingInfo, llmResponse);

        // Kiểm tra thông tin còn thiếu
        String missingInfo = bookingInfo.getMissingInfo();
        if (missingInfo != null) {
            // Tạo context cho câu hỏi tự nhiên
            Map<String, String> context = new HashMap<>();
            if (bookingInfo.getName() != null)
                context.put("name", bookingInfo.getName());
            if (bookingInfo.getPhone() != null)
                context.put("phone", bookingInfo.getPhone());
            if (bookingInfo.getService() != null)
                context.put("service", bookingInfo.getService());
            if (bookingInfo.getDate() != null)
                context.put("date", bookingInfo.getDate().toString());
            if (bookingInfo.getTime() != null)
                context.put("time", bookingInfo.getTime().toString());

            // Tạo câu hỏi tự nhiên
            String naturalQuestion = intentAnalysisService.createNaturalResponse(missingInfo, context);
            response.put("fulfillmentText", naturalQuestion);
            return response;
        }

        // Nếu đã có đủ thông tin, tạo response xác nhận
        if (bookingInfo.isComplete()) {
            String confirmationMessage = createConfirmationMessage(bookingInfo);
            response.put("fulfillmentText", confirmationMessage);
            response.put("bookingInfo", bookingInfo);
        }

        return response;
    }

    private BookingInfo getBookingInfoFromSession(ChatSession session) {
        Map<String, String> context = chatService.getSessionContext(session);
        BookingInfo bookingInfo = new BookingInfo();

        if (context != null) {
            bookingInfo.setName(context.getOrDefault("name", ""));
            bookingInfo.setPhone(context.getOrDefault("phone", ""));
            bookingInfo.setService(context.getOrDefault("service", ""));

            String dateStr = context.getOrDefault("date", "");
            String timeStr = context.getOrDefault("time", "");

            if (!dateStr.isEmpty()) {
                try {
                    bookingInfo.setDate(LocalDate.parse(dateStr));
                } catch (Exception e) {
                    log.error("Error parsing date: ", e);
                }
            }

            if (!timeStr.isEmpty()) {
                try {
                    bookingInfo.setTime(LocalTime.parse(timeStr));
                } catch (Exception e) {
                    log.error("Error parsing time: ", e);
                }
            }
        }

        return bookingInfo;
    }

    private void updateBookingInfoFromLlmResponse(BookingInfo bookingInfo, LlmIntentResponse llmResponse) {
        if (llmResponse == null)
            return;

        if (llmResponse.getService() != null && llmResponse.getService().getName() != null) {
            bookingInfo.setService(llmResponse.getService().getName());
        }

        if (llmResponse.getTime() != null) {
            // Sử dụng IntentAnalysisService để phân tích thời gian
            String dateTimeStr = llmResponse.getTime().getDate() + " " + llmResponse.getTime().getTime();
            Map<String, String> parsedDateTime = intentAnalysisService.parseDateTime(dateTimeStr);

            if (!parsedDateTime.containsKey("error")) {
                try {
                    bookingInfo.setDate(LocalDate.parse(parsedDateTime.get("date")));
                    bookingInfo.setTime(LocalTime.parse(parsedDateTime.get("time")));
                } catch (Exception e) {
                    log.error("Error parsing date/time from LLM: ", e);
                }
            }
        }
    }

    private String createConfirmationMessage(BookingInfo bookingInfo) {
        StringBuilder message = new StringBuilder("Vui lòng xác nhận thông tin đặt lịch:\n");
        message.append("Tên: ").append(bookingInfo.getName()).append("\n");
        message.append("Số điện thoại: ").append(bookingInfo.getPhone()).append("\n");
        message.append("Dịch vụ: ").append(bookingInfo.getService()).append("\n");
        message.append("Thời gian: ")
                .append(bookingInfo.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .append(" lúc ")
                .append(bookingInfo.getTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .append("\n\nBạn có muốn xác nhận đặt lịch không?");

        return message.toString();
    }
}