-- Clear existing data
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
DELETE FROM users;
DELETE FROM role_permission;
DELETE FROM permissions;
DELETE FROM roles;

-- Check if the haircut_services table has duration_minutes column
-- If not, add it (migration from older structure)
SET @dbname = 'haircut';
SET @tablename = 'haircut_services';
SET @columnname = 'duration_minutes';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname) AND
      (TABLE_NAME = @tablename) AND
      (COLUMN_NAME = @columnname)
  ) > 0,
  "SELECT 1",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " INT NOT NULL DEFAULT 30")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add Roles
INSERT INTO roles (id, created_at, updated_at, role_name, description) VALUES
  (1, NOW(), NOW(), 'ADMIN', 'Quản trị viên hệ thống'),
  (2, NOW(), NOW(), 'BARBER', 'Thợ cắt tóc'),
  (3, NOW(), NOW(), 'USER', 'Khách hàng');

-- Add Users (password is hashed - actual value is "password123")
INSERT INTO users (id, created_at, updated_at, email, fullname, password, phone_number, username, role_id) VALUES
  (1, NOW(), NOW(), 'admin@haircut.com', 'Admin User', '$2a$10$NZOh4t.fREwdxr8kj6qGMO4RQu1Vr1.Mh.JY.FxJCFqKGGGDRYpae', '0901234567', 'admin', 1),
  (2, NOW(), NOW(), 'barber1@haircut.com', 'Nguyễn Văn A', '$2a$10$NZOh4t.fREwdxr8kj6qGMO4RQu1Vr1.Mh.JY.FxJCFqKGGGDRYpae', '0901234568', 'barber1', 2),
  (3, NOW(), NOW(), 'barber2@haircut.com', 'Trần Thị B', '$2a$10$NZOh4t.fREwdxr8kj6qGMO4RQu1Vr1.Mh.JY.FxJCFqKGGGDRYpae', '0901234569', 'barber2', 2),
  (4, NOW(), NOW(), 'barber3@haircut.com', 'Lê Văn C', '$2a$10$NZOh4t.fREwdxr8kj6qGMO4RQu1Vr1.Mh.JY.FxJCFqKGGGDRYpae', '0901234570', 'barber3', 2),
  (5, NOW(), NOW(), 'user1@example.com', 'Phạm Văn D', '$2a$10$NZOh4t.fREwdxr8kj6qGMO4RQu1Vr1.Mh.JY.FxJCFqKGGGDRYpae', '0901234571', 'user1', 3),
  (6, NOW(), NOW(), 'user2@example.com', 'Võ Thị E', '$2a$10$NZOh4t.fREwdxr8kj6qGMO4RQu1Vr1.Mh.JY.FxJCFqKGGGDRYpae', '0901234572', 'user2', 3),
  (7, NOW(), NOW(), 'user3@example.com', 'Hoàng Văn F', '$2a$10$NZOh4t.fREwdxr8kj6qGMO4RQu1Vr1.Mh.JY.FxJCFqKGGGDRYpae', '0901234573', 'user3', 3);

-- Add Barbers
INSERT INTO barbers (id, created_at, updated_at, name, email, phone, bio, position, avatar_url, is_active, is_available_for_booking, start_working_hour, end_working_hour) VALUES
  (1, NOW(), NOW(), 'Nguyễn Văn A', 'barber1@haircut.com', '0901234568', 'Thợ cắt tóc chuyên nghiệp với 10 năm kinh nghiệm', 'Senior Stylist', 'https://example.com/avatars/barber1.jpg', 1, 1, '08:00:00', '18:00:00'),
  (2, NOW(), NOW(), 'Trần Thị B', 'barber2@haircut.com', '0901234569', 'Chuyên gia màu sắc và uốn tóc', 'Color Specialist', 'https://example.com/avatars/barber2.jpg', 1, 1, '09:00:00', '19:00:00'),
  (3, NOW(), NOW(), 'Lê Văn C', 'barber3@haircut.com', '0901234570', 'Thợ cắt tóc nam đầy sáng tạo', 'Junior Stylist', 'https://example.com/avatars/barber3.jpg', 1, 1, '10:00:00', '20:00:00');

-- Add Haircut Services
INSERT INTO haircut_services (id, created_at, updated_at, name, description, base_price, duration_minutes, image_url, is_active, sort_order) VALUES
  (1, NOW(), NOW(), 'Cắt tóc nam', 'Dịch vụ cắt tóc cơ bản cho nam', 70000.00, 30, 'https://example.com/services/men-haircut.jpg', 1, 1),
  (2, NOW(), NOW(), 'Cắt tóc nữ', 'Dịch vụ cắt tóc cơ bản cho nữ', 120000.00, 45, 'https://example.com/services/women-haircut.jpg', 1, 2),
  (3, NOW(), NOW(), 'Uốn tóc', 'Dịch vụ uốn tóc thời trang', 350000.00, 120, 'https://example.com/services/perm.jpg', 1, 3),
  (4, NOW(), NOW(), 'Nhuộm tóc', 'Dịch vụ nhuộm tóc theo xu hướng', 450000.00, 150, 'https://example.com/services/coloring.jpg', 1, 4),
  (5, NOW(), NOW(), 'Gội đầu', 'Dịch vụ gội đầu massage', 80000.00, 30, 'https://example.com/services/wash.jpg', 1, 5),
  (6, NOW(), NOW(), 'Combo cắt gội', 'Dịch vụ cắt tóc và gội đầu', 120000.00, 60, 'https://example.com/services/combo.jpg', 1, 6),
  (7, NOW(), NOW(), 'Combo làm tóc cao cấp', 'Bao gồm cắt, uốn và nhuộm', 650000.00, 210, 'https://example.com/services/premium.jpg', 1, 7);

-- Connect Barbers to Services
INSERT INTO barber_services (barber_id, service_id) VALUES
  (1, 1), (1, 5), (1, 6), -- Barber 1 can do haircuts and washing
  (2, 2), (2, 3), (2, 4), (2, 5), (2, 7), -- Barber 2 specializes in coloring and styling
  (3, 1), (3, 2), (3, 5), (3, 6); -- Barber 3 does basic cuts

-- Create some appointments
-- Today - Confirmed appointment
INSERT INTO appointments (id, created_at, updated_at, customer_name, customer_phone, customer_email, date, start_time, end_time, status, total_price, notes, barber_id, customer_id, is_reminder_sent, confirmed_at)
VALUES (1, NOW(), NOW(), 'Phạm Văn D', '0901234571', 'user1@example.com', CURDATE(), '10:00:00', '10:30:00', 'CONFIRMED', 70000.00, 'Cắt ngắn hai bên', 1, 5, 0, NOW());

-- Today - Completed appointment
INSERT INTO appointments (id, created_at, updated_at, customer_name, customer_phone, customer_email, date, start_time, end_time, status, total_price, notes, barber_id, customer_id, is_reminder_sent, confirmed_at)
VALUES (2, NOW(), NOW(), 'Võ Thị E', '0901234572', 'user2@example.com', CURDATE(), '14:00:00', '16:30:00', 'COMPLETED', 350000.00, 'Uốn xoăn nhẹ', 2, 6, 1, DATE_SUB(NOW(), INTERVAL 1 HOUR));

-- Tomorrow - Booked appointment
INSERT INTO appointments (id, created_at, updated_at, customer_name, customer_phone, customer_email, date, start_time, end_time, status, total_price, notes, barber_id, customer_id, is_reminder_sent)
VALUES (3, NOW(), NOW(), 'Hoàng Văn F', '0901234573', 'user3@example.com', DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:00:00', '10:00:00', 'BOOKED', 120000.00, 'Cắt ngắn, vuốt wax', 3, 7, 0);

-- Tomorrow - Another booked appointment
INSERT INTO appointments (id, created_at, updated_at, customer_name, customer_phone, customer_email, date, start_time, end_time, status, total_price, notes, barber_id, is_reminder_sent)
VALUES (4, NOW(), NOW(), 'Khách hàng mới', '0909888777', 'newcustomer@example.com', DATE_ADD(CURDATE(), INTERVAL 1 DAY), '15:00:00', '17:30:00', 'BOOKED', 450000.00, 'Nhuộm màu nâu đỏ', 2, 0);

-- Next week - Booked appointment
INSERT INTO appointments (id, created_at, updated_at, customer_name, customer_phone, customer_email, date, start_time, end_time, status, total_price, notes, barber_id, is_reminder_sent)
VALUES (5, NOW(), NOW(), 'Khách đặt trước', '0905111222', 'advance@example.com', DATE_ADD(CURDATE(), INTERVAL 7 DAY), '11:00:00', '13:30:00', 'BOOKED', 650000.00, 'Combo full, làm tóc cưới', 2, 0);

-- Previous day - Cancelled appointment
INSERT INTO appointments (id, created_at, updated_at, customer_name, customer_phone, customer_email, date, start_time, end_time, status, total_price, notes, barber_id, customer_id, is_reminder_sent, cancelled_at, cancellation_reason)
VALUES (6, NOW(), NOW(), 'Phạm Văn D', '0901234571', 'user1@example.com', DATE_SUB(CURDATE(), INTERVAL 1 DAY), '16:00:00', '16:30:00', 'CANCELLED', 70000.00, 'Cắt tóc đơn giản', 1, 5, 0, DATE_SUB(NOW(), INTERVAL 2 DAY), 'Khách hàng có việc bận');

-- Connect appointments to services
INSERT INTO appointment_services (appointment_id, service_id, quantity) VALUES
  (1, 1, 1), -- Basic men's haircut
  (2, 3, 1), -- Perm
  (3, 6, 1), -- Combo cut and wash
  (4, 4, 1), -- Coloring
  (5, 7, 1), -- Premium combo
  (6, 1, 1); -- Basic men's haircut (cancelled)

-- Add some payments
INSERT INTO payments (id, created_at, updated_at, amount, payment_method, status, transaction_reference, appointment_id) VALUES
  (1, NOW(), NOW(), 70000, 'CASH', 'COMPLETED', 'CASH-001', 1),
  (2, NOW(), NOW(), 350000, 'CARD', 'COMPLETED', 'CARD-TX-002', 2),
  (3, NOW(), NOW(), 120000, 'TRANSFER', 'PENDING', 'TRANSFER-003', 3); 