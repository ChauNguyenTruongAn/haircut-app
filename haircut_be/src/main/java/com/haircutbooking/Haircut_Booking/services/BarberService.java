package com.haircutbooking.Haircut_Booking.services;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.haircutbooking.Haircut_Booking.domain.Barber;
import com.haircutbooking.Haircut_Booking.exception.ResourceNotFoundException;
import com.haircutbooking.Haircut_Booking.repositories.BarberRepository;

import lombok.RequiredArgsConstructor;

/**
 * Dịch vụ quản lý thợ cắt tóc (barber)
 */
@Service
@RequiredArgsConstructor
public class BarberService {

    private static final Logger logger = LoggerFactory.getLogger(BarberService.class);

    private final BarberRepository barberRepository;

    /**
     * Lấy tất cả thợ cắt tóc đang hoạt động
     */
    public List<Barber> getAllActiveBarbers() {
        return barberRepository.findByIsActiveTrueOrderByNameAsc();
    }

    /**
     * Lấy tất cả thợ cắt tóc có thể đặt lịch
     */
    public List<Barber> getAllAvailableBarbers() {
        return barberRepository.findAllAvailableBarbers();
    }

    /**
     * Lấy thợ cắt tóc theo ID
     */
    public Optional<Barber> getBarberById(Long id) {
        return barberRepository.findByIdAndIsActiveTrue(id);
    }

    /**
     * Lấy danh sách thợ cắt tóc cung cấp một dịch vụ cụ thể
     */
    public List<Barber> getBarbersByService(Long serviceId) {
        return barberRepository.findBarbersOfferingService(serviceId);
    }

    /**
     * Lưu thông tin thợ cắt tóc (thêm mới hoặc cập nhật)
     */
    @Transactional
    public Barber saveBarber(Barber barber) {
        logger.info("Saving barber: {}", barber.getName());
        return barberRepository.save(barber);
    }

    /**
     * Hủy kích hoạt (xóa mềm) một thợ cắt tóc
     */
    @Transactional
    public void deactivateBarber(Long id) {
        Barber barber = barberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Barber not found with id: " + id));

        barber.setIsActive(false);
        barber.setIsAvailableForBooking(false);
        barberRepository.save(barber);
    }
}
