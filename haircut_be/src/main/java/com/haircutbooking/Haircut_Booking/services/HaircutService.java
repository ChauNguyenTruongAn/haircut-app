package com.haircutbooking.Haircut_Booking.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.haircutbooking.Haircut_Booking.domain.Barber;
import com.haircutbooking.Haircut_Booking.domain.HaircutOption;
import com.haircutbooking.Haircut_Booking.domain.RequestDTO.AddServiceRequest;
import com.haircutbooking.Haircut_Booking.dto.ServiceDTO;
import com.haircutbooking.Haircut_Booking.repositories.AppointmentRepository;
import com.haircutbooking.Haircut_Booking.repositories.BarberRepository;
import com.haircutbooking.Haircut_Booking.repositories.HaircutOptionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HaircutService {

    private static final Logger logger = LoggerFactory.getLogger(HaircutService.class);

    private final AppointmentRepository appointmentRepository;
    private final HaircutOptionRepository haircutOptionRepository;
    private final BarberRepository barberRepository;
    private final BookingService bookingService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Lấy danh sách dịch vụ có sẵn
     */
    public String getServicesDescription() {
        try {
            List<HaircutOption> services = haircutOptionRepository.findByIsActiveTrueOrderByNameAsc();
            if (services.isEmpty()) {
                return "Hiện tại chúng tôi chưa có thông tin dịch vụ. Vui lòng liên hệ trực tiếp.";
            }

            StringBuilder sb = new StringBuilder("An Barber hiện cung cấp các dịch vụ:\n");
            for (HaircutOption service : services) {
                sb.append("- ").append(service.getName()).append(": ")
                        .append(formatPrice(service.getBasePrice())).append("\n");
            }

            return sb.toString();
        } catch (Exception e) {
            logger.error("Error getting services description", e);
            return "Không thể lấy thông tin dịch vụ. Vui lòng thử lại sau.";
        }
    }

    /**
     * Lấy thông tin giá cho một dịch vụ cụ thể
     */
    public String getServicePrice(String serviceName) {
        try {
            Optional<HaircutOption> serviceOpt = haircutOptionRepository
                    .findByNameAndIsActiveTrue(serviceName);

            if (serviceOpt.isPresent()) {
                HaircutOption service = serviceOpt.get();
                return String.format("Giá dịch vụ %s là %s.",
                        service.getName(),
                        formatPrice(service.getBasePrice()));
            } else {
                // Tìm dịch vụ tương tự
                List<HaircutOption> similarServices = haircutOptionRepository
                        .searchByNameContaining(serviceName);

                if (!similarServices.isEmpty()) {
                    HaircutOption service = similarServices.get(0);
                    return String.format("Chúng tôi có dịch vụ %s với giá chỉ từ %s.",
                            service.getName(),
                            formatPrice(service.getBasePrice()));
                } else {
                    return "Chúng tôi không tìm thấy dịch vụ " + serviceName
                            + ". Vui lòng kiểm tra lại tên dịch vụ hoặc liên hệ trực tiếp.";
                }
            }
        } catch (Exception e) {
            logger.error("Error getting service price for " + serviceName, e);
            return "Không thể lấy thông tin giá dịch vụ. Vui lòng thử lại sau.";
        }
    }

    /**
     * Kiểm tra tính khả dụng cho một ngày và giờ cụ thể
     */
    public String checkAvailability(String dateStr, String timeStr, String serviceName) {
        try {
            // Parse thời gian
            LocalDate date = LocalDate.parse(dateStr);
            LocalTime time = LocalTime.parse(timeStr);

            // Tìm dịch vụ
            Optional<HaircutOption> serviceOpt = haircutOptionRepository.findByNameAndIsActiveTrue(serviceName);
            if (serviceOpt.isEmpty()) {
                List<HaircutOption> similarServices = haircutOptionRepository.searchByNameContaining(serviceName);
                if (similarServices.isEmpty()) {
                    return "Không tìm thấy dịch vụ " + serviceName + ". Vui lòng chọn dịch vụ khác.";
                }
                serviceOpt = Optional.of(similarServices.get(0));
            }

            // Kiểm tra barber khả dụng
            List<Barber> availableBarbers = barberRepository.findAvailableBarbersAtDateTime(date, time);
            // Kiểm tra thời gian đã đặt chưa
            boolean isTimeSlotBooked = bookingService.isTimeSlotBooked(date, time, serviceOpt.get().getId());

            boolean isAvailable = !availableBarbers.isEmpty() && !isTimeSlotBooked;

            if (isAvailable) {
                return String.format(
                        "Chúng tôi có thể phục vụ dịch vụ %s vào ngày %s lúc %s. Bạn có muốn đặt lịch không?",
                        serviceOpt.get().getName(),
                        date.format(DATE_FORMATTER),
                        time.format(TIME_FORMATTER));
            } else {
                // Tìm khung giờ trống tiếp theo
                LocalTime nextAvailableTime = bookingService.findNextAvailableTimeSlot(date, time);
                if (nextAvailableTime != null) {
                    return String.format("Rất tiếc, khung giờ %s vào ngày %s đã kín lịch. " +
                            "Tuy nhiên, chúng tôi có thể phục vụ bạn vào lúc %s. Bạn có muốn đặt lịch vào thời gian này không?",
                            time.format(TIME_FORMATTER),
                            date.format(DATE_FORMATTER),
                            nextAvailableTime.format(TIME_FORMATTER));
                } else {
                    return String.format("Rất tiếc, ngày %s đã kín lịch. Vui lòng chọn ngày khác.",
                            date.format(DATE_FORMATTER));
                }
            }
        } catch (Exception e) {
            logger.error("Error checking availability", e);
            return "Có lỗi xảy ra khi kiểm tra lịch trống. Vui lòng thử lại sau.";
        }
    }

    /**
     * Đặt lịch cắt tóc
     */
    public String bookAppointment(Map<String, String> parameters) {
        return bookingService.processBookingFromChatbot(parameters);
    }

    // Helper methods
    private String formatPrice(BigDecimal price) {
        return String.format("%,.0fđ", price);
    }

    // Admin service methods
    public ServiceDTO addService(AddServiceRequest request) {
        HaircutOption service = HaircutOption.builder()
                .name(request.getName())
                .description(request.getDescription())
                .basePrice(request.getBasePrice())
                .durationMinutes(request.getDurationMinutes())
                .imageUrl(request.getImageUrl())
                .isActive(true)
                .sortOrder(request.getSortOrder())
                .build();

        HaircutOption savedService = haircutOptionRepository.save(service);
        return convertToServiceDTO(savedService);
    }

    public void deleteService(Long id) {
        haircutOptionRepository.deleteById(id);
    }

    public ServiceDTO editService(Long id, AddServiceRequest request) {
        HaircutOption service = haircutOptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        service.setName(request.getName());
        service.setDescription(request.getDescription());
        service.setBasePrice(request.getBasePrice());
        service.setDurationMinutes(request.getDurationMinutes());
        service.setImageUrl(request.getImageUrl());
        service.setSortOrder(request.getSortOrder());

        HaircutOption savedService = haircutOptionRepository.save(service);
        return convertToServiceDTO(savedService);
    }

    public ServiceDTO getServiceById(Long id) {
        HaircutOption service = haircutOptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found"));
        return convertToServiceDTO(service);
    }

    public List<ServiceDTO> getAllServices() {
        return haircutOptionRepository.findByIsActiveTrueOrderByNameAsc().stream()
                .map(this::convertToServiceDTO)
                .collect(Collectors.toList());
    }

    private ServiceDTO convertToServiceDTO(HaircutOption service) {
        return ServiceDTO.builder()
                .id(service.getId())
                .name(service.getName())
                .description(service.getDescription())
                .basePrice(service.getBasePrice())
                .durationMinutes(service.getDurationMinutes())
                .imageUrl(service.getImageUrl())
                .isActive(service.getIsActive())
                .sortOrder(service.getSortOrder())
                .build();
    }
}