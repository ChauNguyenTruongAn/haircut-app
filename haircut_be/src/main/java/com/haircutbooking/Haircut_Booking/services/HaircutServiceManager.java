package com.haircutbooking.Haircut_Booking.services;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.haircutbooking.Haircut_Booking.domain.HaircutOption;
import com.haircutbooking.Haircut_Booking.exception.ResourceNotFoundException;
import com.haircutbooking.Haircut_Booking.repositories.HaircutOptionRepository;

import lombok.RequiredArgsConstructor;

/**
 * Dịch vụ quản lý các dịch vụ cắt tóc
 */
@Service
@RequiredArgsConstructor
public class HaircutServiceManager {

    private static final Logger logger = LoggerFactory.getLogger(HaircutServiceManager.class);

    private final HaircutOptionRepository serviceRepository;

    /**
     * Lấy tất cả dịch vụ hoạt động
     */
    public List<HaircutOption> getAllActiveServices() {
        return serviceRepository.findByIsActiveTrueOrderBySortOrderAsc();
    }

    /**
     * Lấy dịch vụ theo ID
     */
    public Optional<HaircutOption> getServiceById(Long id) {
        return serviceRepository.findById(id)
                .filter(HaircutOption::getIsActive);
    }

    /**
     * Tìm kiếm dịch vụ theo từ khóa
     */
    public List<HaircutOption> searchServices(String keyword) {
        return serviceRepository.searchByNameContaining(keyword);
    }

    /**
     * Lấy dịch vụ của một thợ cắt tóc cụ thể
     */
    public List<HaircutOption> getServicesByBarber(Long barberId) {
        return serviceRepository.findServicesByBarberId(barberId);
    }

    /**
     * Lưu hoặc cập nhật dịch vụ
     */
    @Transactional
    public HaircutOption saveService(HaircutOption service) {
        logger.info("Saving service: {}", service.getName());
        return serviceRepository.save(service);
    }

    /**
     * Vô hiệu hóa dịch vụ
     */
    @Transactional
    public void deactivateService(Long id) {
        logger.info("Deactivating service with id: {}", id);

        HaircutOption service = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + id));

        service.setIsActive(false);
        serviceRepository.save(service);
    }
}