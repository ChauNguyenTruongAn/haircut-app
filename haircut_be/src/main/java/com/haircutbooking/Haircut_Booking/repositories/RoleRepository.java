package com.haircutbooking.Haircut_Booking.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.haircutbooking.Haircut_Booking.domain.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleName(String roleName);
}
