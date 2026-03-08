# Ứng dụng đặt lịch cắt tóc

Hệ thống đặt lịch cắt tóc tích hỗ trợ trả lời tự động thông qua Facebook Messenger.
Khách hàng có thể đặt lịch thông qua Messenger sử dụng Dialogflow NLP để xử lý yêu cầu.
Ứng dụng chủ yếu quản lý lịch hẹn của khác hàng. Người dùng chủ yếu của ứng dụng là chủ cửa hàng.


## Luồng hoạt động của hệ thống khi khách hàng đặt lịch thông qua Facebook Messenger:

1. Khách hàng gửi tin nhắn qua Facebook Messenger
   Ví dụ: *"Tôi muốn đặt lịch cắt tóc"*

2. Facebook Messenger chuyển tin nhắn đến **Dialogflow Agent**

3. Dialogflow phân tích nội dung tin nhắn và nhận diện **Intent**
   Ví dụ: `booking_appointment`

4. Dialogflow gửi **Webhook Request** đến endpoint được cấu hình

5. **Ngrok** chuyển tiếp request từ internet đến **Spring Boot Backend**

6. Backend xử lý logic nghiệp vụ:

   * Kiểm tra lịch trống
   * Tạo lịch hẹn mới
   * Lưu dữ liệu vào hệ thống

7. Backend trả về **Webhook Response**

8. Dialogflow định dạng phản hồi và gửi lại cho khách hàng qua Messenger

---

## Mô tả hệ thống
<img width="945" height="1371" alt="image" src="https://github.com/user-attachments/assets/626e3cc7-9d48-4e0e-87ac-467f93e6ec96" />

# Mobile Application (Frontend)

## Technology

* Flutter SDK
* Dart

## Main Features

* Giao diện người dùng thân thiện
* Hỗ trợ nền tảng:
  * Android
* Quản lý trạng thái ứng dụng bằng **Provider Pattern**
* Đặt lịch cắt tóc
* Xem và quản lý lịch hẹn
* Quản lý thông tin tài khoản người dùng

---

# Backend API (Spring Boot)

## Technology

* Java 17
* Spring Boot 3.4.4
* Spring Data JPA
* Spring Security

## Layered Architecture

### Controller Layer

* Xử lý HTTP requests
* Kiểm tra và validate dữ liệu đầu vào
* Định nghĩa REST API endpoints

### Service Layer

* Chứa business logic
* Quản lý transaction

### Repository Layer

* Truy cập dữ liệu
* Thực hiện các truy vấn JPA

### Domain Layer

* Entity classes
* Data models

---

## Security

Hệ thống sử dụng **Spring Security kết hợp JWT Authentication**.

Các cơ chế bảo mật bao gồm:

* JWT Authentication
* Role-Based Access Control
* Password hashing với **BCrypt**

### System Roles

* ADMIN
* BARBER
* USER

Các API endpoint được bảo vệ bằng annotation:

```
@PreAuthorize
```

---

# Chatbot

## Dialogflow

Dialogflow được sử dụng để xử lý **Natural Language Processing (NLP)** cho các tin nhắn tiếng Việt.

### Intents

* booking_appointment
* cancel_appointment
* check_status

### Entities

* services
* date
* time
* date-time

### Contexts

Contexts được sử dụng để quản lý **luồng hội thoại** giữa chatbot và người dùng.

---

## Facebook Messenger

Facebook Messenger đóng vai trò là giao diện giao tiếp giữa khách hàng và chatbot.

Tin nhắn từ Messenger sẽ được gửi đến Dialogflow để phân tích và xử lý.

---

## Webhook

Backend cung cấp webhook endpoint để Dialogflow gọi khi cần xử lý dữ liệu.

Quy trình thực hiện:

1. Dialogflow gửi webhook request
2. Spring Boot Controller nhận request
3. Service layer xử lý logic nghiệp vụ
4. Backend trả response
5. Dialogflow gửi phản hồi về Messenger

---

# Ngrok

Ngrok được sử dụng trong quá trình phát triển để expose local server ra internet.

* Tạo secure tunnel từ internet đến local development server
* Cung cấp HTTPS endpoint cho Dialogflow webhook
* Hỗ trợ theo dõi và debug request

## Configuration

* Tunnel đến Spring Boot server (port 8080)
* HTTPS endpoint với custom subdomain
* Logging và inspection request/response

---

## Cách cài đặt Dialogflow
[Hướng dẫn cài đặt](https://github.com/ChauNguyenTruongAn/haircut-app/blob/main/haircut_be/DIALOGFLOW_SETUP.md)

## Hình ảnh sản phẩm
<img width="417" height="770" alt="image" src="https://github.com/user-attachments/assets/708438ce-c52b-4951-b605-48ac4047f895" />
<img width="417" height="770" alt="image" src="https://github.com/user-attachments/assets/fe43074a-473a-43f2-b163-8756ace7a820" />
<img width="417" height="770" alt="image" src="https://github.com/user-attachments/assets/3e04f257-c612-425a-a486-26522d66bf5f" />
<img width="417" height="770" alt="image" src="https://github.com/user-attachments/assets/d9d1f99f-27bf-4ddc-8fdb-9a978e9a4b98" />
<img width="417" height="770" alt="image" src="https://github.com/user-attachments/assets/fdfc20c8-6b43-4919-ad56-996c897ea8d6" />
<img width="1036" height="770" alt="image" src="https://github.com/user-attachments/assets/79fb59bc-e67b-4739-90e3-1d819e2c970a" />
<img width="1036" height="770" alt="image" src="https://github.com/user-attachments/assets/96d5b692-fc07-460e-a3a2-5e9117c8353d" />







