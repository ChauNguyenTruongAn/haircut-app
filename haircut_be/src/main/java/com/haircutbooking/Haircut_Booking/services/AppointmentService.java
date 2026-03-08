package com.haircutbooking.Haircut_Booking.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.haircutbooking.Haircut_Booking.domain.Appointment;
import com.haircutbooking.Haircut_Booking.domain.Barber;
import com.haircutbooking.Haircut_Booking.domain.HaircutOption;
import com.haircutbooking.Haircut_Booking.domain.User;
import com.haircutbooking.Haircut_Booking.domain.RequestDTO.AddAppointmentRequest;
import com.haircutbooking.Haircut_Booking.domain.ResponseDTO.AppointmentResponse;
import com.haircutbooking.Haircut_Booking.dto.AppointmentDTO;
import com.haircutbooking.Haircut_Booking.exception.ResourceNotFoundException;
import com.haircutbooking.Haircut_Booking.repositories.AppointmentRepository;
import com.haircutbooking.Haircut_Booking.repositories.BarberRepository;
import com.haircutbooking.Haircut_Booking.repositories.HaircutOptionRepository;
import com.haircutbooking.Haircut_Booking.repositories.UserRepository;
import com.haircutbooking.Haircut_Booking.util.AppointmentStatus;

import lombok.RequiredArgsConstructor;

/**
 * Service để quản lý các cuộc hẹn
 */
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointmentRepository;
    private final BarberRepository barberRepository;
    private final UserRepository userRepository;
    private final HaircutOptionRepository haircutServiceRepository;

    /**
     * Lấy tất cả cuộc hẹn
     */
    public List<AppointmentResponse> getAllAppointments() {
        List<Appointment> appointments = appointmentRepository.findAll();
        return appointments.stream()
                .map(this::convertToAppointmentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Chuyển đổi từ Entity sang DTO
     */
    private AppointmentResponse convertToAppointmentResponse(Appointment appointment) {
        return AppointmentResponse.builder()
                .userId(appointment.getCustomer() != null ? appointment.getCustomer().getId() : null)
                .barberId(appointment.getBarber().getId())
                .appointmentDate(appointment.getDate())
                .appointmentTime(appointment.getStartTime())
                .appointmentTimeEnd(appointment.getEndTime())
                .status(appointment.getStatus().name())
                .note(appointment.getNotes())
                .services(appointment.getServices())
                .build();
    }

    /**
     * Lấy cuộc hẹn theo ID
     */
    public AppointmentResponse getAppointmentById(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));
        return convertToAppointmentResponse(appointment);
    }

    /**
     * Lấy danh sách cuộc hẹn trong một khoảng thời gian
     */
    public List<Appointment> getAppointmentsByDateRange(LocalDate startDate, LocalDate endDate) {
        return appointmentRepository.findByDateBetween(startDate, endDate);
    }

    /**
     * Lấy danh sách cuộc hẹn của một thợ cắt tóc trong một ngày
     */
    public List<Appointment> getBarberAppointments(Long barberId, LocalDate date) {
        return appointmentRepository.findByBarberAndDate(barberId, date);
    }

    /**
     * Lấy danh sách cuộc hẹn của một khách hàng
     */
    public List<Appointment> getCustomerAppointments(Long customerId) {
        return appointmentRepository.findByCustomerId(customerId);
    }

    /**
     * Tìm kiếm cuộc hẹn theo số điện thoại
     */
    public List<Appointment> findAppointmentsByPhone(String phone) {
        return appointmentRepository.findByCustomerPhone(phone);
    }

    /**
     * Tạo cuộc hẹn mới
     */
    @Transactional
    public AppointmentResponse createAppointment(AddAppointmentRequest request) {
        logger.info("Creating new appointment for date: {}, time: {}",
                request.getAppointmentDate(), request.getAppointmentTime());

        // Tìm thợ cắt tóc
        Barber barber = barberRepository.findById(request.getBarberId())
                .orElseThrow(() -> new ResourceNotFoundException("Barber not found"));

        // Tìm khách hàng
        User customer = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Tìm các dịch vụ
        Set<HaircutOption> services = request.getServices().keySet().stream()
                .map(id -> haircutServiceRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + id)))
                .collect(Collectors.toSet());

        // Tính tổng giá
        BigDecimal totalPrice = services.stream()
                .map(HaircutOption::getBasePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Kiểm tra thợ cắt tóc có rảnh không
        if (!isBarberAvailable(barber, request.getAppointmentDate(), request.getAppointmentTime(),
                request.getAppointmentTimeEnd())) {
            throw new IllegalStateException("Barber is not available at the requested time");
        }

        // Tạo appointment
        Appointment appointment = Appointment.builder()
                .customer(customer)
                .barber(barber)
                .date(request.getAppointmentDate())
                .startTime(request.getAppointmentTime())
                .endTime(request.getAppointmentTimeEnd())
                .customerName(customer.getFullName())
                .customerPhone(customer.getPhoneNumber())
                .status(request.getStatus())
                .notes(request.getNote())
                .totalPrice(totalPrice)
                .services(services)
                .isReminderSent(false)
                .build();

        Appointment savedAppointment = appointmentRepository.save(appointment);

        // Chuyển đổi sang response DTO
        return AppointmentResponse.builder()
                .userId(savedAppointment.getCustomer().getId())
                .barberId(savedAppointment.getBarber().getId())
                .appointmentDate(savedAppointment.getDate())
                .appointmentTime(savedAppointment.getStartTime())
                .appointmentTimeEnd(savedAppointment.getEndTime())
                .status(savedAppointment.getStatus().name())
                .note(savedAppointment.getNotes())
                .services(savedAppointment.getServices())
                .build();
    }

    /**
     * Cập nhật trạng thái cuộc hẹn thành CONFIRMED
     */
    @Transactional
    public Appointment confirmAppointment(Long appointmentId) {
        logger.info("Confirming appointment: {}", appointmentId);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setConfirmedAt(LocalDateTime.now());

        return appointmentRepository.save(appointment);
    }

    /**
     * Hủy cuộc hẹn
     */
    @Transactional
    public Appointment cancelAppointment(Long appointmentId, String reason) {
        logger.info("Cancelling appointment: {}", appointmentId);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelledAt(LocalDateTime.now());
        appointment.setCancellationReason(reason);

        return appointmentRepository.save(appointment);
    }

    /**
     * Hoàn thành cuộc hẹn
     */
    @Transactional
    public Appointment completeAppointment(Long appointmentId) {
        logger.info("Completing appointment: {}", appointmentId);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        appointment.setStatus(AppointmentStatus.COMPLETED);

        return appointmentRepository.save(appointment);
    }

    /**
     * Tính thời gian kết thúc dựa trên thời gian bắt đầu và các dịch vụ
     */
    private LocalTime calculateEndTime(LocalTime startTime, Set<HaircutOption> services) {
        int totalDurationMinutes = services.stream()
                .mapToInt(service -> service.getDurationMinutes())
                .sum();

        return startTime.plusMinutes(totalDurationMinutes);
    }

    /**
     * Kiểm tra thợ cắt tóc có rảnh không
     */
    public boolean isBarberAvailable(Barber barber, LocalDate date, LocalTime requestedStart, LocalTime requestedEnd) {
        // Kiểm tra trong giờ làm việc của barber
        if (requestedStart.isBefore(barber.getStartWorkingHour()) || requestedEnd.isAfter(barber.getEndWorkingHour())) {
            return false;
        }

        // Kiểm tra không đặt lịch trong quá khứ
        LocalDateTime requestedDateTime = LocalDateTime.of(date, requestedStart);
        if (requestedDateTime.isBefore(LocalDateTime.now())) {
            return false;
        }

        // Kiểm tra trùng với các lịch hẹn khác
        List<Appointment> existingAppointments = appointmentRepository.findByBarberAndDate(barber.getId(), date);

        for (Appointment existing : existingAppointments) {
            // Bỏ qua các lịch hẹn đã hủy
            if (existing.getStatus() == AppointmentStatus.CANCELLED) {
                continue;
            }

            LocalTime existingStart = existing.getStartTime();
            LocalTime existingEnd = existing.getEndTime();

            // Kiểm tra trùng thời gian
            if (!(requestedEnd.isBefore(existingStart) || requestedStart.isAfter(existingEnd))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Cập nhật thông tin cuộc hẹn
     */
    @Transactional
    public AppointmentResponse updateAppointment(Long appointmentId, AddAppointmentRequest request) {
        logger.info("Updating appointment: {}", appointmentId);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        // Cập nhật thông tin thợ cắt tóc nếu có thay đổi
        if (!appointment.getBarber().getId().equals(request.getBarberId())) {
            Barber barber = barberRepository.findById(request.getBarberId())
                    .orElseThrow(() -> new ResourceNotFoundException("Barber not found"));
            appointment.setBarber(barber);
        }

        // Kiểm tra thời gian mới có hợp lệ không
        boolean isTimeChanged = !appointment.getDate().equals(request.getAppointmentDate())
                || !appointment.getStartTime().equals(request.getAppointmentTime())
                || !appointment.getEndTime().equals(request.getAppointmentTimeEnd());

        if (isTimeChanged) {
            // Kiểm tra xem thợ cắt tóc có rảnh không
            if (!isBarberAvailable(appointment.getBarber(),
                    request.getAppointmentDate(),
                    request.getAppointmentTime(),
                    request.getAppointmentTimeEnd(),
                    appointmentId)) {
                throw new IllegalStateException("Barber is not available at the requested time");
            }

            // Cập nhật thời gian
            appointment.setDate(request.getAppointmentDate());
            appointment.setStartTime(request.getAppointmentTime());
            appointment.setEndTime(request.getAppointmentTimeEnd());
        }

        // Cập nhật dịch vụ
        Set<HaircutOption> services = request.getServices().keySet().stream()
                .map(id -> haircutServiceRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + id)))
                .collect(Collectors.toSet());

        appointment.setServices(services);

        // Tính tổng giá dựa trên dịch vụ mới
        BigDecimal totalPrice = services.stream()
                .map(HaircutOption::getBasePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        appointment.setTotalPrice(totalPrice);

        // Cập nhật trạng thái
        appointment.setStatus(request.getStatus());

        // Cập nhật ghi chú
        appointment.setNotes(request.getNote());

        Appointment savedAppointment = appointmentRepository.save(appointment);
        return convertToAppointmentResponse(savedAppointment);
    }

    /**
     * Kiểm tra thợ cắt tóc có rảnh không (có loại trừ cuộc hẹn hiện tại khi cập
     * nhật)
     */
    public boolean isBarberAvailable(Barber barber, LocalDate date, LocalTime requestedStart,
            LocalTime requestedEnd, Long currentAppointmentId) {
        // Kiểm tra trong giờ làm việc của barber
        if (requestedStart.isBefore(barber.getStartWorkingHour()) || requestedEnd.isAfter(barber.getEndWorkingHour())) {
            return false;
        }

        // Kiểm tra không đặt lịch trong quá khứ
        LocalDateTime requestedDateTime = LocalDateTime.of(date, requestedStart);
        if (requestedDateTime.isBefore(LocalDateTime.now())) {
            return false;
        }

        // Kiểm tra trùng với các lịch hẹn khác
        List<Appointment> existingAppointments = appointmentRepository.findByBarberAndDate(barber.getId(), date);

        for (Appointment existing : existingAppointments) {
            // Bỏ qua cuộc hẹn hiện tại và các lịch hẹn đã hủy
            if (existing.getId().equals(currentAppointmentId) || existing.getStatus() == AppointmentStatus.CANCELLED) {
                continue;
            }

            LocalTime existingStart = existing.getStartTime();
            LocalTime existingEnd = existing.getEndTime();

            // Kiểm tra trùng thời gian
            if (!(requestedEnd.isBefore(existingStart) || requestedStart.isAfter(existingEnd))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Xóa cuộc hẹn
     */
    @Transactional
    public void deleteAppointment(Long id) {
        logger.info("Deleting appointment: {}", id);

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));

        appointmentRepository.delete(appointment);
    }

    /**
     * Lấy danh sách cuộc hẹn trong một ngày cụ thể
     * Được sử dụng bởi ứng dụng Flutter để hiển thị lịch hẹn theo ngày
     */
    public List<AppointmentDTO> getAppointmentsByDate(LocalDate date) {
        logger.info("Fetching appointments for date: {}", date);
        List<Appointment> appointments = appointmentRepository.findByDate(date);

        return appointments.stream()
                .map(this::convertToAppointmentDTO)
                .collect(Collectors.toList());
    }

    /**
     * Chuyển đổi Entity sang DTO cho ứng dụng mobile
     */
    private AppointmentDTO convertToAppointmentDTO(Appointment appointment) {
        Set<HaircutOption> services = appointment.getServices();

        AppointmentDTO dto = new AppointmentDTO();
        dto.setId(appointment.getId());
        dto.setCustomerName(appointment.getCustomerName());
        dto.setCustomerPhone(appointment.getCustomerPhone());
        dto.setDate(appointment.getDate());
        dto.setStartTime(appointment.getStartTime());
        dto.setEndTime(appointment.getEndTime());
        dto.setStatus(appointment.getStatus().name());
        dto.setNotes(appointment.getNotes());
        dto.setTotalPrice(appointment.getTotalPrice().doubleValue());
        dto.setBarberId(appointment.getBarber().getId());

        if (appointment.getBarber() != null) {
            dto.setBarberName(appointment.getBarber().getName());
        }

        if (appointment.getCustomer() != null) {
            dto.setUserId(appointment.getCustomer().getId());
        }

        // Chuyển danh sách dịch vụ thành danh sách ID
        Set<Long> serviceIds = services.stream()
                .map(HaircutOption::getId)
                .collect(Collectors.toSet());
        dto.setServiceIds(serviceIds);

        // Thêm chi tiết dịch vụ nếu cần
        List<HaircutOption> serviceList = services.stream()
                .map(service -> {
                    HaircutOption s = new HaircutOption();
                    s.setId(service.getId());
                    s.setName(service.getName());
                    s.setBasePrice(service.getBasePrice());
                    s.setDurationMinutes(service.getDurationMinutes());
                    return s;
                })
                .collect(Collectors.toList());
        dto.setServices(serviceList);

        return dto;
    }

    /**
     * Cập nhật trạng thái của cuộc hẹn
     */
    @Transactional
    public AppointmentResponse updateAppointmentStatus(Long appointmentId, AppointmentStatus status, String reason) {
        logger.info("Updating appointment status: {}, new status: {}", appointmentId, status);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));

        appointment.setStatus(status);

        // Update relevant status-specific fields
        LocalDateTime now = LocalDateTime.now();
        switch (status) {
            case CONFIRMED:
                appointment.setConfirmedAt(now);
                break;
            case CANCELLED:
                appointment.setCancelledAt(now);
                if (reason != null && !reason.isEmpty()) {
                    appointment.setCancellationReason(reason);
                }
                break;
            case COMPLETED:
                appointment.setCompletedAt(now);
                break;
            default:
                break;
        }

        Appointment savedAppointment = appointmentRepository.save(appointment);
        return convertToAppointmentResponse(savedAppointment);
    }
}