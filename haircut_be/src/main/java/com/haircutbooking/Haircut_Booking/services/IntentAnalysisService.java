package com.haircutbooking.Haircut_Booking.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntentAnalysisService {
    private final LlmIntentService llmIntentService;
    private final ObjectMapper objectMapper;

    public Map<String, String> parseDateTime(String dateTimeStr) {
        Map<String, String> result = new HashMap<>();

        try {
            // Tạo prompt cho LLM để phân tích thời gian
            LocalDateTime now = LocalDateTime.now();
            String currentDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String currentTime = now.format(DateTimeFormatter.ofPattern("HH:mm"));

            String prompt = "Bạn là trợ lý AI chuyên hỗ trợ đặt lịch cho tiệm tóc. Nhiệm vụ của bạn là " +
                    "phân tích và chuẩn hóa thông tin thời gian được nhập dưới dạng ngôn ngữ tự nhiên thành " +
                    "định dạng chuẩn nhằm phục vụ quá trình đặt lịch. \n\n" +
                    "Thông tin thời gian hiện tại: " + currentDate + " " + currentTime + "\n" +
                    "Thông tin thời gian cần phân tích: " + dateTimeStr + "\n\n" +
                    "Quy tắc xử lý và lưu ý:\n" +
                    "- Các từ ngữ mô tả thời gian như 'ngày mai', 'tuần sau', 'thứ X' cần được tính từ ngày hiện tại ("
                    + currentDate + ").\n" +
                    "- Hãy cân nhắc các biểu thức tự nhiên khác (ví dụ: 'tối nay', 'sáng mai', 'hôm sau') nếu có xuất hiện.\n"
                    +
                    "- Phân loại khung giờ:\n" +
                    "    * Sáng: từ 06:00 đến 11:59\n" +
                    "    * Chiều: từ 12:00 đến 17:59\n" +
                    "    * Tối: từ 18:00 đến 22:00\n" +
                    "- Nếu không thể nhận diện hay chuẩn hóa được thông tin thời gian, hãy trả về giá trị 'isValid' là false kèm theo thông báo lỗi rõ ràng.\n\n"
                    +
                    "Đầu ra phải tuân theo định dạng JSON sau (chỉ trả về JSON, không thêm bất kỳ văn bản hay giải thích nào):\n"
                    +
                    "{\n" +
                    "  \"date\": \"(định dạng yyyy-MM-dd)\",\n" +
                    "  \"time\": \"(định dạng HH:mm)\",\n" +
                    "  \"isValid\": true/false,\n" +
                    "  \"error\": \"(thông báo lỗi nếu có)\",\n" +
                    "  \"timePeriod\": \"(sáng/chiều/tối)\"\n" +
                    "}";

            // Gọi LLM để phân tích thời gian
            String llmResponse = llmIntentService.callHuggingFaceAPI(prompt, "");

            // Parse kết quả
            JsonNode jsonNode = objectMapper.readTree(llmResponse);

            // Kiểm tra tính hợp lệ của kết quả
            if (!jsonNode.has("isValid")) {
                result.put("error", "Kết quả phân tích không hợp lệ");
                return result;
            }

            if (!jsonNode.get("isValid").asBoolean()) {
                result.put("error",
                        jsonNode.has("error") ? jsonNode.get("error").asText() : "Không thể phân tích thời gian");
                return result;
            }

            // Kiểm tra các trường bắt buộc
            if (!jsonNode.has("date") || !jsonNode.has("time")) {
                result.put("error", "Thiếu thông tin ngày hoặc giờ");
                return result;
            }

            String date = jsonNode.get("date").asText();
            String time = jsonNode.get("time").asText();

            // Kiểm tra định dạng ngày
            try {
                LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                result.put("error", "Định dạng ngày không hợp lệ: " + date);
                return result;
            }

            // Kiểm tra định dạng giờ
            try {
                LocalTime.parse(time);
            } catch (DateTimeParseException e) {
                result.put("error", "Định dạng giờ không hợp lệ: " + time);
                return result;
            }

            // Lưu kết quả
            result.put("date", date);
            result.put("time", time);
            if (jsonNode.has("timePeriod")) {
                result.put("timePeriod", jsonNode.get("timePeriod").asText());
            }

        } catch (Exception e) {
            log.error("Error parsing date time: ", e);
            result.put("error", "Có lỗi xảy ra khi phân tích thời gian: " + e.getMessage());
        }

        return result;
    }

    public boolean analyzeConfirmationIntent(String userMessage) {
        try {
            // Tạo prompt cho LLM để phân tích ý định xác nhận
            String prompt = "Bạn là trợ lý AI cho tiệm tóc. Hãy phân tích tin nhắn của khách hàng và xác định họ có đồng ý đặt lịch không.\n\n"
                    +
                    "Tin nhắn của khách hàng: \"" + userMessage + "\"\n\n" +
                    "Lưu ý:\n" +
                    "- Các từ khóa đồng ý: 'đồng ý', 'ok', 'được', 'chấp nhận', 'xác nhận', 'okay', 'okie', 'okey', 'okay', 'okie', 'okey'\n"
                    +
                    "- Các từ khóa từ chối: 'không', 'không đồng ý', 'từ chối', 'hủy', 'bỏ', 'thôi'\n" +
                    "- Cần xem xét ngữ cảnh và ý định thực sự của khách hàng\n\n" +
                    "Trả về JSON theo cấu trúc sau:\n" +
                    "{\n" +
                    "  \"isConfirmed\": true/false,\n" +
                    "  \"confidence\": 0-1,\n" +
                    "  \"reason\": \"lý do nếu không đồng ý\"\n" +
                    "}\n" +
                    "Chỉ trả về JSON, không thêm bất kỳ giải thích hay văn bản nào khác.";

            // Gọi LLM để phân tích ý định
            String llmResponse = llmIntentService.callHuggingFaceAPI(prompt, "");

            // Parse kết quả
            JsonNode jsonNode = objectMapper.readTree(llmResponse);

            // Kiểm tra độ tin cậy của kết quả
            if (jsonNode.has("confidence") && jsonNode.get("confidence").asDouble() >= 0.7) {
                return jsonNode.get("isConfirmed").asBoolean();
            }

            return false;
        } catch (Exception e) {
            log.error("Error analyzing confirmation intent: ", e);
            return false;
        }
    }

    public String createNaturalResponse(String missingInfo, Map<String, String> context) {
        try {
            // Tạo prompt cho LLM để tạo câu trả lời tự nhiên
            String prompt = "Bạn là trợ lý AI cho tiệm tóc. Hãy tạo một câu hỏi thân thiện và tự nhiên để hỏi khách hàng về "
                    + missingInfo + ".\n\n" +
                    "Thông tin hiện tại:\n";

            if (context != null) {
                for (Map.Entry<String, String> entry : context.entrySet()) {
                    prompt += entry.getKey() + ": " + entry.getValue() + "\n";
                }
            }

            prompt += "\nHãy tạo một câu hỏi thân thiện và tự nhiên, không quá dài, không quá ngắn.";

            // Gọi LLM để tạo câu trả lời
            return llmIntentService.callHuggingFaceAPI(prompt, "");
        } catch (Exception e) {
            log.error("Error creating natural response: ", e);
            return "Bạn vui lòng cho tôi biết " + missingInfo + ".";
        }
    }

    public Map<String, Object> analyzeIntent(String userMessage) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Tạo prompt cho LLM để phân tích ý định
            String prompt = "Bạn là trợ lý AI cho tiệm tóc. Hãy phân tích tin nhắn của khách hàng và xác định ý định của họ.\n\n"
                    + "Tin nhắn của khách hàng: \"" + userMessage + "\"\n\n"
                    + "Các ý định có thể:\n"
                    + "1. haircut.welcome - Chào hỏi, bắt đầu cuộc trò chuyện\n"
                    + "2. haircut.booking.service - Hỏi về dịch vụ cắt tóc\n"
                    + "3. haircut.booking.date-time - Hỏi về thời gian đặt lịch\n"
                    + "4. haircut.booking.info - Cung cấp thông tin đặt lịch\n"
                    + "5. haircut.booking.confirm - Xác nhận đặt lịch\n"
                    + "6. haircut.booking.cancel - Hủy đặt lịch\n"
                    + "7. haircut.default - Các ý định khác\n\n"
                    + "Trả về JSON theo cấu trúc sau:\n"
                    + "{\n"
                    + "  \"intent\": \"tên ý định\",\n"
                    + "  \"confidence\": 0-1,\n"
                    + "  \"parameters\": {\n"
                    + "    \"service\": \"tên dịch vụ nếu có\",\n"
                    + "    \"date\": \"ngày nếu có\",\n"
                    + "    \"time\": \"giờ nếu có\",\n"
                    + "    \"phone\": \"số điện thoại nếu có\",\n"
                    + "    \"name\": \"tên nếu có\"\n"
                    + "  }\n"
                    + "}\n"
                    + "Chỉ trả về JSON, không thêm bất kỳ giải thích hay văn bản nào khác.";

            // Gọi LLM để phân tích ý định
            String llmResponse = llmIntentService.callHuggingFaceAPI(prompt, "");

            // Parse kết quả
            JsonNode jsonNode = objectMapper.readTree(llmResponse);

            // Lấy thông tin ý định
            if (jsonNode.has("intent")) {
                result.put("intent", jsonNode.get("intent").asText());
            }

            // Lấy độ tin cậy
            if (jsonNode.has("confidence")) {
                result.put("confidence", jsonNode.get("confidence").asDouble());
            }

            // Lấy các tham số
            if (jsonNode.has("parameters")) {
                Map<String, String> parameters = new HashMap<>();
                JsonNode paramsNode = jsonNode.get("parameters");

                if (paramsNode.has("service")) {
                    parameters.put("service", paramsNode.get("service").asText());
                }
                if (paramsNode.has("date")) {
                    parameters.put("date", paramsNode.get("date").asText());
                }
                if (paramsNode.has("time")) {
                    parameters.put("time", paramsNode.get("time").asText());
                }
                if (paramsNode.has("phone")) {
                    parameters.put("phone", paramsNode.get("phone").asText());
                }
                if (paramsNode.has("name")) {
                    parameters.put("name", paramsNode.get("name").asText());
                }

                result.put("parameters", parameters);
            }

        } catch (Exception e) {
            log.error("Error analyzing intent: ", e);
            result.put("intent", "haircut.default");
            result.put("confidence", 0.0);
            result.put("parameters", new HashMap<>());
        }

        return result;
    }

    public Map<String, String> parseCustomerInfo(String userMessage) {
        Map<String, String> result = new HashMap<>();

        try {
            // Tạo prompt cho LLM để phân tích thông tin khách hàng
            String prompt = "Bạn là trợ lý AI cho tiệm tóc. Hãy phân tích tin nhắn của khách hàng và trích xuất tên và số điện thoại.\n\n"
                    +
                    "Tin nhắn của khách hàng: \"" + userMessage + "\"\n\n" +
                    "Lưu ý:\n" +
                    "- Tên có thể là tên đầy đủ hoặc tên riêng\n" +
                    "- Số điện thoại phải bắt đầu bằng 0 và có 10-11 chữ số\n" +
                    "- Tin nhắn có thể có nhiều định dạng khác nhau\n\n" +
                    "Trả về JSON theo cấu trúc sau:\n" +
                    "{\n" +
                    "  \"name\": \"tên khách hàng\",\n" +
                    "  \"phone\": \"số điện thoại\",\n" +
                    "  \"isValid\": true/false,\n" +
                    "  \"error\": \"thông báo lỗi nếu có\"\n" +
                    "}\n" +
                    "Chỉ trả về JSON, không thêm bất kỳ giải thích hay văn bản nào khác.";

            // Gọi LLM để phân tích thông tin
            String llmResponse = llmIntentService.callHuggingFaceAPI(prompt, "");

            // Parse kết quả
            JsonNode jsonNode = objectMapper.readTree(llmResponse);

            // Kiểm tra tính hợp lệ của kết quả
            if (!jsonNode.has("isValid")) {
                result.put("error", "Kết quả phân tích không hợp lệ");
                return result;
            }

            if (!jsonNode.get("isValid").asBoolean()) {
                result.put("error", jsonNode.has("error") ? jsonNode.get("error").asText()
                        : "Không thể phân tích thông tin khách hàng");
                return result;
            }

            // Kiểm tra các trường bắt buộc
            if (!jsonNode.has("name") || !jsonNode.has("phone")) {
                result.put("error", "Thiếu thông tin tên hoặc số điện thoại");
                return result;
            }

            String name = jsonNode.get("name").asText();
            String phone = jsonNode.get("phone").asText();

            // Kiểm tra tính hợp lệ của số điện thoại
            if (!isValidPhoneNumber(phone)) {
                result.put("error",
                        "Số điện thoại không hợp lệ. Vui lòng cung cấp số điện thoại 10-11 chữ số bắt đầu bằng số 0.");
                return result;
            }

            // Lưu kết quả
            result.put("name", name);
            result.put("phone", phone);

        } catch (Exception e) {
            log.error("Error parsing customer info: ", e);
            result.put("error", "Có lỗi xảy ra khi phân tích thông tin khách hàng: " + e.getMessage());
        }

        return result;
    }

    private boolean isValidPhoneNumber(String phone) {
        // Kiểm tra số điện thoại bắt đầu bằng 0 và có 10-11 chữ số
        return phone != null && phone.matches("^0[0-9]{9,10}$");
    }

    public Map<String, Object> parseCancelBookingIntent(String userMessage) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Tạo prompt cho LLM để phân tích ý định hủy đơn
            String prompt = "Bạn là trợ lý AI cho tiệm tóc. Hãy phân tích tin nhắn của khách hàng và xác định:\n" +
                    "1. Họ có muốn hủy đơn không\n" +
                    "2. Mã đơn hẹn\n\n" +
                    "Tin nhắn của khách hàng: \"" + userMessage + "\"\n\n" +
                    "Lưu ý:\n" +
                    "- Các từ khóa hủy đơn: 'hủy', 'hủy đơn', 'hủy hẹn', 'hủy lịch', 'cancel', 'cancel booking'\n" +
                    "- Mã đơn hẹn thường là một số\n" +
                    "- Cần xác định rõ ý định hủy đơn và mã đơn nếu có\n\n" +
                    "Trả về JSON theo cấu trúc sau:\n" +
                    "{\n" +
                    "  \"isCancelIntent\": true/false,\n" +
                    "  \"bookingId\": \"mã đơn nếu có\",\n" +
                    "  \"confidence\": 0-1,\n" +
                    "  \"error\": \"thông báo lỗi nếu có\"\n" +
                    "}\n" +
                    "Chỉ trả về JSON, không thêm bất kỳ giải thích hay văn bản nào khác.";

            // Gọi LLM để phân tích ý định
            String llmResponse = llmIntentService.callHuggingFaceAPI(prompt, "");

            // Parse kết quả
            JsonNode jsonNode = objectMapper.readTree(llmResponse);

            // Kiểm tra tính hợp lệ của kết quả
            if (!jsonNode.has("isCancelIntent")) {
                result.put("error", "Kết quả phân tích không hợp lệ");
                return result;
            }

            // Lấy thông tin ý định hủy đơn
            boolean isCancelIntent = jsonNode.get("isCancelIntent").asBoolean();
            result.put("isCancelIntent", isCancelIntent);

            // Lấy độ tin cậy
            if (jsonNode.has("confidence")) {
                result.put("confidence", jsonNode.get("confidence").asDouble());
            }

            // Lấy mã đơn nếu có
            if (jsonNode.has("bookingId") && !jsonNode.get("bookingId").asText().isEmpty()) {
                result.put("bookingId", jsonNode.get("bookingId").asText());
            }

            // Kiểm tra lỗi nếu có
            if (jsonNode.has("error")) {
                result.put("error", jsonNode.get("error").asText());
            }

        } catch (Exception e) {
            log.error("Error parsing cancel booking intent: ", e);
            result.put("error", "Có lỗi xảy ra khi phân tích ý định hủy đơn: " + e.getMessage());
        }

        return result;
    }

    public List<String> parseServices(String userMessage) {
        List<String> services = new ArrayList<>();

        try {
            // Tạo prompt cho LLM để phân tích các dịch vụ
            String prompt = "Bạn là trợ lý AI chuyên hỗ trợ đặt lịch cho tiệm tóc. Nhiệm vụ của bạn là " +
                    "phân tích và trích xuất tên các dịch vụ từ tin nhắn của khách hàng.\n\n" +
                    "Tin nhắn cần phân tích: " + userMessage + "\n\n" +
                    "Quy tắc xử lý:\n" +
                    "- Mỗi dịch vụ phải được trích xuất riêng biệt\n" +
                    "- Chỉ trích xuất tên dịch vụ, không bao gồm các từ khác\n" +
                    "- Nếu không tìm thấy dịch vụ nào, trả về mảng rỗng\n\n" +
                    "Đầu ra phải tuân theo định dạng JSON sau (chỉ trả về JSON, không thêm bất kỳ văn bản hay giải thích nào):\n"
                    +
                    "{\n" +
                    "  \"services\": [\"tên dịch vụ 1\", \"tên dịch vụ 2\", ...]\n" +
                    "}";

            // Gọi LLM để phân tích dịch vụ
            String llmResponse = llmIntentService.callHuggingFaceAPI(prompt, "");

            // Parse kết quả
            JsonNode jsonNode = objectMapper.readTree(llmResponse);

            // Kiểm tra và lấy danh sách dịch vụ
            if (jsonNode.has("services") && jsonNode.get("services").isArray()) {
                for (JsonNode serviceNode : jsonNode.get("services")) {
                    services.add(serviceNode.asText());
                }
            }
        } catch (Exception e) {
            log.error("Error parsing services from message: " + userMessage, e);
        }

        return services;
    }
}