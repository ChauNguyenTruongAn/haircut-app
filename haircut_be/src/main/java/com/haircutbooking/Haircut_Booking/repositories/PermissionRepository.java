package com.haircutbooking.Haircut_Booking.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.haircutbooking.Haircut_Booking.domain.Permission;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

}
