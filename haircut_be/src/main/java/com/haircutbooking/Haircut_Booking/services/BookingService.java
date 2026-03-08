package com.haircutbooking.Haircut_Booking.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import com.haircutbooking.Haircut_Booking.domain.Role;
import com.haircutbooking.Haircut_Booking.domain.TimeSlot;
import com.haircutbooking.Haircut_Booking.domain.User;
import com.haircutbooking.Haircut_Booking.dto.AppointmentDTO;
import com.haircutbooking.Haircut_Booking.dto.AvailabilityDTO;
import com.haircutbooking.Haircut_Booking.exception.ResourceNotFoundException;
import com.haircutbooking.Haircut_Booking.repositories.AppointmentRepository;
import com.haircutbooking.Haircut_Booking.repositories.BarberRepository;
import com.haircutbooking.Haircut_Booking.repositories.HaircutOptionRepository;
import com.haircutbooking.Haircut_Booking.repositories.RoleRepository;
import com.haircutbooking.Haircut_Booking.repositories.UserRepository;
import com.haircutbooking.Haircut_Booking.util.AppointmentStatus;

import lombok.RequiredArgsConstructor;

/**
 * Dịch vụ xử lý tất cả các hoạt động liên quan đến đặt lịch
 */
@Service
@RequiredArgsConstructor
public class BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    private final AppointmentRepository appointmentRepository;
    private final BarberRepository barberRepository;
    private final HaircutOptionRepository haircutServiceRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public AvailabilityDTO checkAvailability(LocalDate date, LocalTime time, Long serviceId) {
        logger.info("Checking availability for date: {}, time: {}, service: {}", date, time, serviceId);

        HaircutOption service = haircutServiceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        // Kiểm tra xem có barber nào có thể thực hiện dịch vụ này không
        List<Barber> barbersForService = barberRepository.findBarbersOfferingService(serviceId);
        if (barbersForService.isEmpty()) {
            return AvailabilityDTO.builder()
                    .date(date)
                    .time(time)
                    .isAvailable(false)
                    .barbersAvailable(0)
                    .availableBarbers(List.of())
                    .build();
        }

        // Kiểm tra có barber làm việc vào thời gian này không
        List<Barber> availableBarbers = barberRepository.findAvailableBarbersAtDateTime(date, time);
        availableBarbers.retainAll(barbersForService);

        // Lọc ra các barber có lịch trống
        List<Barber> trulyAvailableBarbers = availableBarbers.stream()
                .filter(barber -> !isTimeSlotBooked(date, time, barber.getId()))
                .collect(Collectors.toList());

        boolean isAvailable = !trulyAvailableBarbers.isEmpty();

        // Nếu không có barber nào rảnh, tìm khung giờ trống tiếp theo
        LocalTime nextAvailableTime = null;
        if (!isAvailable) {
            nextAvailableTime = findNextAvailableTimeSlotForService(date, time, serviceId);
        }

        return AvailabilityDTO.builder()
                .date(date)
                .time(time)
                .isAvailable(isAvailable)
                .barbersAvailable(trulyAvailableBarbers.size())
                .nextAvailableTime(nextAvailableTime)
                .availableBarbers(trulyAvailableBarbers)
                .build();
    }

    /**
     * Tìm khung giờ trống tiếp theo cho một dịch vụ cụ thể
     */
    private LocalTime findNextAvailableTimeSlotForService(LocalDate date, LocalTime startTime, Long serviceId) {
        // Lấy danh sách các khung giờ đã được sử dụng trong ngày
        List<LocalTime> bookedStartTimes = appointmentRepository.findUsedTimeSlotsInDay(date);

        // Khung giờ làm việc từ 8:00 đến 18:00, với các khoảng thời gian 30 phút
        List<LocalTime> allPossibleTimeSlots = new ArrayList<>();
        LocalTime workStart = LocalTime.of(8, 0);
        LocalTime workEnd = LocalTime.of(18, 0);

        // Tạo danh sách tất cả các khung giờ có thể trong ngày
        for (LocalTime time = workStart; !time.isAfter(workEnd.minusMinutes(30)); time = time.plusMinutes(30)) {
            allPossibleTimeSlots.add(time);
        }

        // Lọc các khung giờ đã đặt
        List<LocalTime> availableTimeSlots = new ArrayList<>(allPossibleTimeSlots);
        for (LocalTime bookedTime : bookedStartTimes) {
            availableTimeSlots.remove(bookedTime);
        }

        // Tìm khung giờ trống tiếp theo sau startTime mà có thợ có thể thực hiện dịch
        // vụ
        for (LocalTime time : availableTimeSlots) {
            if (!time.isBefore(startTime)) {
                List<Barber> availableBarbers = barberRepository.findAvailableBarbersAtDateTime(date, time);
                List<Barber> barbersForService = barberRepository.findBarbersOfferingService(serviceId);
                availableBarbers.retainAll(barbersForService);

                if (!availableBarbers.isEmpty() && !isTimeSlotBooked(date, time, availableBarbers.get(0).getId())) {
                    return time;
                }
            }
        }

        return null;
    }

    /**
     * Tạo một cuộc hẹn mới
     */
    @Transactional
    public Appointment createAppointment(AppointmentDTO appointmentDTO) {
        logger.info("Creating new appointment for date: {}, time: {}",
                appointmentDTO.getDate(), appointmentDTO.getStartTime());

        // Tìm thợ cắt tóc
        List<Barber> availableBarbers = barberRepository.findAvailableBarbersAtDateTime(appointmentDTO.getDate(),
                appointmentDTO.getStartTime());
        if (availableBarbers.isEmpty()) {
            throw new ResourceNotFoundException("No available barbers at this time");
        }
        Barber barber = availableBarbers.get(0);

        // Tìm các dịch vụ
        Set<HaircutOption> services = appointmentDTO.getServiceIds().stream()
                .map(id -> haircutServiceRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + id)))
                .collect(Collectors.toSet());

        // Tính tổng giá và thời gian kết thúc
        BigDecimal totalPrice = services.stream()
                .map(HaircutOption::getBasePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalTime endTime = calculateEndTime(appointmentDTO.getStartTime(), services);

        // Tìm hoặc tạo User dựa trên số điện thoại
        User customer = null;
        String customerPhone = appointmentDTO.getCustomerPhone();

        // Kiểm tra thông tin khách hàng
        if (customerPhone == null || customerPhone.isEmpty()) {
            throw new IllegalArgumentException("Số điện thoại khách hàng không được để trống");
        }

        // Xử lý tên khách hàng
        final String customerName = appointmentDTO.getCustomerName().isEmpty() ? "Khách hàng " + customerPhone
                : appointmentDTO.getCustomerName();

        // Tìm user hiện có hoặc tạo mới
        customer = userRepository.findByPhoneNumber(customerPhone)
                .map(existingUser -> {
                    // Cập nhật senderId nếu có
                    if (appointmentDTO.getSenderId() != null && !appointmentDTO.getSenderId().isEmpty()) {
                        existingUser.setSenderId(appointmentDTO.getSenderId());
                        return userRepository.save(existingUser);
                    }
                    return existingUser;
                })
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .phoneNumber(customerPhone)
                            .fullName(customerName)
                            .senderId(appointmentDTO.getSenderId())
                            .role(roleRepository.findByRoleName("USER")
                                    .orElseGet(() -> roleRepository.save(Role.builder()
                                            .roleName("USER")
                                            .build())))
                            .build();
                    return userRepository.save(newUser);
                });

        // Tạo appointment
        Appointment appointment = Appointment.builder()
                .customer(customer)
                .barber(barber)
                .date(appointmentDTO.getDate())
                .startTime(appointmentDTO.getStartTime())
                .endTime(endTime)
                .customerName(customerName)
                .customerPhone(customerPhone)
                .status(AppointmentStatus.BOOKED)
                .notes(appointmentDTO.getNotes())
                .totalPrice(totalPrice)
                .services(services)
                .isReminderSent(false)
                .build();

        return appointmentRepository.save(appointment);
    }

    /**
     * Xác nhận cuộc hẹn
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
     * Lấy tất cả cuộc hẹn trong một phạm vi ngày
     */
    public List<Appointment> getAppointmentsByDateRange(LocalDate startDate, LocalDate endDate) {
        return appointmentRepository.findByDateBetween(startDate, endDate);
    }

    /**
     * Lấy tất cả cuộc hẹn của một thợ cắt tóc trong một ngày
     */
    public List<Appointment> getBarberAppointments(Long barberId, LocalDate date) {
        return appointmentRepository.findByBarberAndDate(barberId, date);
    }

    /**
     * Lấy lịch sử cuộc hẹn của một khách hàng
     */
    public List<Appointment> getCustomerAppointments(Long customerId) {
        return appointmentRepository.findByCustomerId(customerId);
    }

    /**
     * Tìm cuộc hẹn theo số điện thoại
     */
    public List<Appointment> findAppointmentsByPhone(String phone) {
        return appointmentRepository.findByCustomerPhone(phone);
    }

    /**
     * Phân trang tất cả cuộc hẹn
     */
    public Page<Appointment> getAllAppointments(Pageable pageable) {
        return appointmentRepository.findAll(pageable);
    }

    private LocalTime calculateEndTime(LocalTime startTime, Set<HaircutOption> services) {
        int totalDurationMinutes = services.stream()
                .mapToInt(service -> service.getDurationMinutes())
                .sum();

        return startTime.plusMinutes(totalDurationMinutes);
    }

    /**
     * Chuyển đổi Map từ Dialogflow thành dữ liệu đặt lịch
     */
    public String processBookingFromChatbot(Map<String, String> parameters) {
        try {
            String customerName = parameters.getOrDefault("name", "");
            String phone = parameters.getOrDefault("phone", "");
            String serviceStr = parameters.getOrDefault("service", "");
            String dateStr = parameters.getOrDefault("date", "");
            String timeStr = parameters.getOrDefault("time", "");

            if (customerName.isEmpty() || phone.isEmpty() || serviceStr.isEmpty() || dateStr.isEmpty()
                    || timeStr.isEmpty()) {
                return "Vui lòng cung cấp đầy đủ thông tin để đặt lịch (tên, số điện thoại, dịch vụ, ngày và giờ).";
            }

            // Tìm dịch vụ
            Optional<HaircutOption> serviceOpt = haircutServiceRepository.findByNameAndIsActiveTrue(serviceStr);
            if (serviceOpt.isEmpty()) {
                List<HaircutOption> similarServices = haircutServiceRepository.searchByNameContaining(serviceStr);
                if (similarServices.isEmpty()) {
                    return "Không tìm thấy dịch vụ " + serviceStr + ". Vui lòng chọn dịch vụ khác.";
                }
                serviceOpt = Optional.of(similarServices.get(0));
            }

            // Parse date và time
            LocalDate date;
            LocalTime time;

            try {
                date = LocalDate.parse(dateStr);
                time = LocalTime.parse(timeStr);
            } catch (Exception e) {
                return "Định dạng ngày giờ không hợp lệ. Vui lòng thử lại.";
            }

            // Tìm barber có lịch trống
            List<Barber> availableBarbers = barberRepository.findAvailableBarbersAtDateTime(date, time);
            if (availableBarbers.isEmpty()) {
                return "Không có thợ cắt tóc nào rảnh vào thời gian này. Vui lòng chọn thời gian khác.";
            }

            // Tạo appointment DTO
            AppointmentDTO appointmentDTO = AppointmentDTO.builder()
                    .barberId(availableBarbers.get(0).getId())
                    .date(date)
                    .startTime(time)
                    .customerName(customerName)
                    .customerPhone(phone)
                    .serviceIds(Set.of(serviceOpt.get().getId()))
                    .build();

            // Tạo appointment
            Appointment appointment = createAppointment(appointmentDTO);

            return String.format(
                    "Đã đặt lịch thành công! Thông tin lịch hẹn:\n" +
                            "- Tên: %s\n" +
                            "- Số điện thoại: %s\n" +
                            "- Dịch vụ: %s\n" +
                            "- Thợ cắt tóc: %s\n" +
                            "- Ngày: %s\n" +
                            "- Giờ: %s\n" +
                            "- Mã đặt lịch: %d\n" +
                            "Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi!",
                    customerName, phone, serviceOpt.get().getName(),
                    appointment.getBarber().getName(),
                    appointment.getDate().format(DATE_FORMATTER),
                    appointment.getStartTime().format(TIME_FORMATTER),
                    appointment.getId());
        } catch (Exception e) {
            logger.error("Error processing booking from chatbot", e);
            return "Có lỗi xảy ra khi đặt lịch. Vui lòng thử lại sau.";
        }
    }

    /**
     * Kiểm tra xem có cuộc hẹn nào trùng với ngày và giờ không
     * 
     * @param date     Ngày đặt lịch
     * @param time     Giờ bắt đầu
     * @param barberId ID của barber cần kiểm tra
     * @return true nếu đã có cuộc hẹn trùng, false nếu chưa có
     */
    public boolean isTimeSlotBooked(LocalDate date, LocalTime time, Long barberId) {
        List<Appointment> appointments = appointmentRepository.findByBarberAndDate(barberId, date);
        for (Appointment appointment : appointments) {
            // Bỏ qua các lịch hẹn đã hủy
            if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
                continue;
            }

            LocalTime startTime = appointment.getStartTime();
            LocalTime endTime = appointment.getEndTime();

            // Kiểm tra xem thời gian yêu cầu có nằm trong khoảng thời gian đã đặt không
            if (!time.isBefore(startTime) && time.isBefore(endTime)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tìm khung giờ trống tiếp theo trong ngày
     * 
     * @param date      Ngày cần tìm
     * @param startTime Thời gian bắt đầu tìm kiếm
     * @return Khung giờ trống tiếp theo hoặc null nếu không tìm thấy
     */
    public LocalTime findNextAvailableTimeSlot(LocalDate date, LocalTime startTime) {
        // Lấy danh sách các khung giờ đã được sử dụng trong ngày
        List<LocalTime> bookedStartTimes = appointmentRepository.findUsedTimeSlotsInDay(date);

        // Khung giờ làm việc từ 8:00 đến 18:00, với các khoảng thời gian 1 giờ
        List<LocalTime> allPossibleTimeSlots = new ArrayList<>();
        LocalTime workStart = LocalTime.of(8, 0);
        LocalTime workEnd = LocalTime.of(18, 0);

        // Tạo danh sách tất cả các khung giờ có thể trong ngày
        for (LocalTime time = workStart; !time.isAfter(workEnd.minusHours(1)); time = time.plusHours(1)) {
            allPossibleTimeSlots.add(time);
        }

        // Lọc các khung giờ đã đặt
        List<LocalTime> availableTimeSlots = new ArrayList<>(allPossibleTimeSlots);
        for (LocalTime bookedTime : bookedStartTimes) {
            availableTimeSlots.remove(bookedTime);
        }

        // Tìm khung giờ trống tiếp theo sau startTime
        return availableTimeSlots.stream()
                .filter(time -> !time.isBefore(startTime))
                .min(LocalTime::compareTo)
                .orElse(null);
    }

    /**
     * Lấy danh sách tất cả các khung giờ trống trong ngày
     * 
     * @param date Ngày cần kiểm tra
     * @return Danh sách các khung giờ trống
     */
    public List<LocalTime> findAllAvailableTimeSlotsInDay(LocalDate date) {
        // Lấy danh sách các khung giờ đã được sử dụng trong ngày
        List<LocalTime> bookedStartTimes = appointmentRepository.findUsedTimeSlotsInDay(date);

        // Khung giờ làm việc từ 8:00 đến 18:00, với các khoảng thời gian 1 giờ
        List<LocalTime> allPossibleTimeSlots = new ArrayList<>();
        LocalTime workStart = LocalTime.of(8, 0);
        LocalTime workEnd = LocalTime.of(18, 0);

        // Tạo danh sách tất cả các khung giờ có thể trong ngày
        for (LocalTime time = workStart; !time.isAfter(workEnd.minusHours(1)); time = time.plusHours(1)) {
            // Kiểm tra xem khung giờ này có khả dụng không (có thợ làm và chưa có lịch đặt)
            if (isTimeSlotAvailable(date, time)) {
                allPossibleTimeSlots.add(time);
            }
        }

        // Lọc các khung giờ đã đặt
        List<LocalTime> availableTimeSlots = new ArrayList<>(allPossibleTimeSlots);
        for (LocalTime bookedTime : bookedStartTimes) {
            availableTimeSlots.remove(bookedTime);
        }

        return availableTimeSlots;
    }

    /**
     * Kiểm tra xem thời gian có khả dụng không (có thợ làm và chưa có lịch trùng)
     * 
     * @param date Ngày cần kiểm tra
     * @param time Thời gian cần kiểm tra
     * @return true nếu khung giờ khả dụng, false nếu đã bị đặt hoặc không có thợ
     *         rảnh
     */
    private boolean isTimeSlotAvailable(LocalDate date, LocalTime time) {
        // Kiểm tra có thợ cắt tóc rảnh không
        List<Barber> availableBarbers = barberRepository.findAvailableBarbersAtDateTime(date, time);

        // Kiểm tra thời gian có trùng lịch không
        boolean isTimeSlotBooked = isTimeSlotBooked(date, time, availableBarbers.get(0).getId());

        return !availableBarbers.isEmpty() && !isTimeSlotBooked;
    }

    /**
     * Lấy danh sách tất cả các khung giờ trống trong ngày
     * 
     * @param date Ngày cần kiểm tra
     * @return Danh sách các khung giờ trống
     */
    public List<TimeSlot> getAvailableTimeSlots(LocalDate date) {
        List<LocalTime> availableTimes = findAllAvailableTimeSlotsInDay(date);
        return availableTimes.stream()
                .map(time -> new TimeSlot(time, time.plusHours(1)))
                .collect(Collectors.toList());
    }

    public HaircutOption findServiceByName(String name) {
        return haircutServiceRepository.findByNameAndIsActiveTrue(name)
                .orElse(null);
    }

    public Appointment createAppointment(String serviceName, LocalDate date, LocalTime time, String phone,
            String name) {
        HaircutOption service = findServiceByName(serviceName);
        if (service == null) {
            return null;
        }

        AppointmentDTO appointmentDTO = new AppointmentDTO();
        appointmentDTO.setServiceIds(Set.of(service.getId()));
        appointmentDTO.setDate(date);
        appointmentDTO.setStartTime(time);
        appointmentDTO.setCustomerPhone(phone);
        appointmentDTO.setCustomerName(name);
        return createAppointment(appointmentDTO);
    }

    public boolean cancelAppointment(Long appointmentId) {
        try {
            Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
            if (appointment != null) {
                appointment.setStatus(AppointmentStatus.CANCELLED);
                appointmentRepository.save(appointment);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Error cancelling appointment: ", e);
            return false;
        }
    }

    public List<Appointment> getAppointmentsByStatus(AppointmentStatus status) {
        return appointmentRepository.findByStatus(status);
    }

    public Double getTotalRevenue(LocalDate startDate, LocalDate endDate) {
        return appointmentRepository.getTotalRevenue(startDate, endDate);
    }

    public Map<String, Double> getRevenueByService(LocalDate startDate, LocalDate endDate) {
        List<Object[]> results = appointmentRepository.getRevenueByService(startDate, endDate);
        Map<String, Double> revenueByService = new HashMap<>();
        for (Object[] result : results) {
            String serviceName = (String) result[0];
            Long amount = (Long) result[1];
            revenueByService.put(serviceName, amount.doubleValue());
        }
        return revenueByService;
    }

    public Map<AppointmentStatus, Long> getAppointmentCountByStatus(LocalDate startDate, LocalDate endDate) {
        List<Object[]> results = appointmentRepository.getAppointmentCountByStatus(startDate, endDate);
        Map<AppointmentStatus, Long> countByStatus = new HashMap<>();
        for (Object[] result : results) {
            AppointmentStatus status = (AppointmentStatus) result[0];
            Long count = (Long) result[1];
            countByStatus.put(status, count);
        }
        return countByStatus;
    }
}