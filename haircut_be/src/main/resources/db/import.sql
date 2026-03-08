-- This script will run automatically when Spring Boot starts with the 'create' or 'create-drop' settings

-- Clear existing data first to avoid duplicates
DELETE FROM appointment_services;
DELETE FROM payments;
DELETE FROM appointments;
DELETE FROM barber_services;
DELETE FROM haircut_services;
DELETE FROM barbers;
DELETE FROM noti_user;
DELETE FROM notifications;
DELETE FROM chat_messages;
DELETE FROM chat_session_context;
DELETE FROM chat_sessions;
DELETE FROM users WHERE username != 'admin';
DELETE FROM role_permission;
DELETE FROM permissions;
DELETE FROM roles WHERE role_name != 'ADMIN';

-- Add Roles if not exists
INSERT INTO roles (id, created_at, updated_at, role_name, description) 
SELECT 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'BARBER', 'Thợ cắt tóc'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'BARBER');

INSERT INTO roles (id, created_at, updated_at, role_name, description) 
SELECT 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'USER', 'Khách hàng'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'USER');

-- Add Barbers
INSERT INTO barbers (id, created_at, updated_at, name, email, phone, bio, position, avatar_url, is_active, is_available_for_booking, start_working_hour, end_working_hour) VALUES
  (1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Nguyễn Văn A', 'barber1@haircut.com', '0901234568', 'Thợ cắt tóc chuyên nghiệp với 10 năm kinh nghiệm', 'Senior Stylist', 'https://example.com/avatars/barber1.jpg', 1, 1, '08:00:00', '18:00:00'),
  (2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Trần Thị B', 'barber2@haircut.com', '0901234569', 'Chuyên gia màu sắc và uốn tóc', 'Color Specialist', 'https://example.com/avatars/barber2.jpg', 1, 1, '09:00:00', '19:00:00'),
  (3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Lê Văn C', 'barber3@haircut.com', '0901234570', 'Thợ cắt tóc nam đầy sáng tạo', 'Junior Stylist', 'https://example.com/avatars/barber3.jpg', 1, 1, '10:00:00', '20:00:00');

-- Add Haircut Services
INSERT INTO haircut_services (id, created_at, updated_at, name, description, base_price, duration_minutes, image_url, is_active, sort_order) VALUES
  (1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Cắt tóc nam', 'Dịch vụ cắt tóc cơ bản cho nam', 70000.00, 30, 'https://example.com/services/men-haircut.jpg', 1, 1),
  (2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Cắt tóc nữ', 'Dịch vụ cắt tóc cơ bản cho nữ', 120000.00, 45, 'https://example.com/services/women-haircut.jpg', 1, 2),
  (3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Uốn tóc', 'Dịch vụ uốn tóc thời trang', 350000.00, 120, 'https://example.com/services/perm.jpg', 1, 3),
  (4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Nhuộm tóc', 'Dịch vụ nhuộm tóc theo xu hướng', 450000.00, 150, 'https://example.com/services/coloring.jpg', 1, 4),
  (5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Gội đầu', 'Dịch vụ gội đầu massage', 80000.00, 30, 'https://example.com/services/wash.jpg', 1, 5),
  (6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Combo cắt gội', 'Dịch vụ cắt tóc và gội đầu', 120000.00, 60, 'https://example.com/services/combo.jpg', 1, 6),
  (7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Combo làm tóc cao cấp', 'Bao gồm cắt, uốn và nhuộm', 650000.00, 210, 'https://example.com/services/premium.jpg', 1, 7);

-- Connect Barbers to Services
INSERT INTO barber_services (barber_id, service_id) VALUES
  (1, 1), (1, 5), (1, 6), -- Barber 1 can do haircuts and washing
  (2, 2), (2, 3), (2, 4), (2, 5), (2, 7), -- Barber 2 specializes in coloring and styling
  (3, 1), (3, 2), (3, 5), (3, 6); -- Barber 3 does basic cuts 