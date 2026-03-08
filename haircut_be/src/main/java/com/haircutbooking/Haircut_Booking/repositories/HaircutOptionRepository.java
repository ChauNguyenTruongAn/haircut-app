package com.haircutbooking.Haircut_Booking.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.haircutbooking.Haircut_Booking.domain.HaircutOption;

@Repository
public interface HaircutOptionRepository extends JpaRepository<HaircutOption, Long> {

    List<HaircutOption> findByIsActiveTrueOrderByNameAsc();

    List<HaircutOption> findByIsActiveTrueOrderBySortOrderAsc();

    Optional<HaircutOption> findByNameAndIsActiveTrue(String name);

    @Query("SELECT s FROM HaircutOption s WHERE s.isActive = true AND LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<HaircutOption> searchByNameContaining(@Param("keyword") String keyword);

    @Query(value = "SELECT hs.* FROM haircut_services hs " +
            "JOIN barber_services bs ON hs.id = bs.service_id " +
            "WHERE bs.barber_id = :barberId AND hs.is_active = true " +
            "ORDER BY hs.sort_order ASC", nativeQuery = true)
    List<HaircutOption> findServicesByBarberId(@Param("barberId") Long barberId);
}