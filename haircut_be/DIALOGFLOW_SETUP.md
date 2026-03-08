# Hướng dẫn thiết lập Dialogflow cho hệ thống đặt lịch cắt tóc

## Cài đặt môi trường

1. Sao chép file `.env.example` thành `.env` và cập nhật các giá trị phù hợp:

   ```bash
   cp .env.example .env
   ```

2. Cập nhật URL webhook Dialogflow trong file `.env`:
   ```
   DIALOGFLOW_WEBHOOK_URL=https://your-ngrok-url.ngrok-free.app/webhook/dialogflow
   ```

## Thiết lập Dialogflow

### 1. Tạo agent mới

1. Truy cập [Dialogflow Console](https://dialogflow.cloud.google.com/)
2. Tạo agent mới với tên "HaircutBooking"
3. Chọn ngôn ngữ "Vietnamese" và timezone phù hợp
4. Nhấn "Create"

### 2. Thiết lập Webhook Fulfillment

1. Trong Dialogflow Console, chọn "Fulfillment" từ menu bên trái
2. Bật (Enable) Webhook
3. Nhập URL Webhook: `https://your-ngrok-url.ngrok-free.app/webhook/dialogflow`
4. Nhấn "Save"

### 3. Tạo các Intent

#### Intent: Default Welcome Intent

- Đây là intent mặc định khi người dùng bắt đầu cuộc trò chuyện
- Training phrases:
  - "xin chào"
  - "chào bạn"
  - "hi"
  - "hello"
  - "salon tóc"
- Fulfillment: Bật Webhook Call

#### Intent: Booking

- Training phrases:
  - "tôi muốn đặt lịch"
  - "đặt lịch cắt tóc"
  - "cắt tóc"
  - "tôi muốn uốn tóc"
  - "tôi muốn nhuộm tóc"
- Parameters:
  - `date` (system entity: @sys.date)
  - `time` (system entity: @sys.time)
  - `service` (custom entity: @service-type)
  - `name` (system entity: @sys.person)
  - `phone` (system entity: @sys.phone-number)
- Output Contexts:
  - `booking` (lifespan: 5)
- Fulfillment: Bật Webhook Call

#### Intent: Booking - askForDate (follow-up)

- Parent: Booking
- Input Context: `booking`
- Training phrases:
  - "ngày mai"
  - "thứ 7 này"
  - "ngày 20 tháng 5"
  - "$date"
- Parameters:
  - `date` (system entity: @sys.date)
- Fulfillment: Bật Webhook Call

#### Intent: Booking - askForTime (follow-up)

- Parent: Booking
- Input Context: `booking`
- Training phrases:
  - "9 giờ sáng"
  - "3 giờ chiều"
  - "buổi trưa"
  - "$time"
- Parameters:
  - `time` (system entity: @sys.time)
- Fulfillment: Bật Webhook Call

#### Intent: Booking - askForService (follow-up)

- Parent: Booking
- Input Context: `booking`
- Training phrases:
  - "cắt tóc"
  - "uốn tóc"
  - "nhuộm tóc"
  - "$service"
- Parameters:
  - `service` (custom entity: @service-type)
- Fulfillment: Bật Webhook Call

#### Intent: Booking - askForName (follow-up)

- Parent: Booking
- Input Context: `booking`
- Training phrases:
  - "Tôi là $name"
  - "Tên tôi là $name"
  - "$name"
- Parameters:
  - `name` (system entity: @sys.person)
- Fulfillment: Bật Webhook Call

#### Intent: Booking - askForPhone (follow-up)

- Parent: Booking
- Input Context: `booking`
- Training phrases:
  - "số điện thoại của tôi là $phone"
  - "$phone"
- Parameters:
  - `phone` (system entity: @sys.phone-number)
- Fulfillment: Bật Webhook Call

#### Intent: ServiceInquiry

- Training phrases:
  - "có những dịch vụ nào"
  - "salon có những dịch vụ gì"
  - "dịch vụ cắt tóc của bạn"
- Parameters:
  - `service` (custom entity: @service-type) - optional
- Fulfillment: Bật Webhook Call

#### Intent: PriceInquiry

- Training phrases:
  - "giá cả thế nào"
  - "cắt tóc giá bao nhiêu"
  - "chi phí uốn tóc"
- Parameters:
  - `service` (custom entity: @service-type) - optional
- Fulfillment: Bật Webhook Call

#### Intent: CheckAvailability

- Training phrases:
  - "có lịch trống không"
  - "kiểm tra lịch trống"
  - "thứ 7 có ai rảnh không"
- Parameters:
  - `date` (system entity: @sys.date) - optional
  - `time` (system entity: @sys.time) - optional
- Fulfillment: Bật Webhook Call

#### Intent: CancelBooking

- Training phrases:
  - "tôi muốn hủy lịch"
  - "hủy đặt lịch"
  - "không đến được nữa"
- Parameters:
  - `booking_id` (system entity: @sys.number) - optional
  - `phone` (system entity: @sys.phone-number) - optional
- Fulfillment: Bật Webhook Call

### 4. Tạo Entity tùy chỉnh

#### Entity: service-type

- Giá trị:
  - cắt tóc (synonyms: cắt, tỉa, undercut, mohican)
  - uốn tóc (synonyms: uốn, uốn xoăn, uốn phồng)
  - nhuộm tóc (synonyms: nhuộm, highlight, ombre, balayage)
  - gội đầu (synonyms: gội, xả)
  - dưỡng tóc (synonyms: hấp dầu, phục hồi, ủ tóc)
  - tạo kiểu (synonyms: làm tóc, vuốt tóc, bới tóc)

## Kiểm tra và tích hợp

1. Trong Dialogflow Console, sử dụng chức năng "Try it now" để kiểm tra các intent
2. Theo dõi logs của hệ thống Spring Boot để xác nhận webhooks đang hoạt động chính xác
3. Kiểm tra cơ sở dữ liệu xem các cuộc trò chuyện và thông tin đặt lịch có được lưu trữ đúng cách không

## Mở rộng

Bạn có thể mở rộng hệ thống bằng cách:

1. Tạo thêm các intent để xử lý tình huống đặc biệt
2. Thêm rich responses (như hình ảnh, nút bấm) trong các phản hồi
3. Tích hợp với các nền tảng khác như Facebook Messenger, Zalo, v.v.
4. Triển khai chức năng xác nhận qua SMS hoặc email
