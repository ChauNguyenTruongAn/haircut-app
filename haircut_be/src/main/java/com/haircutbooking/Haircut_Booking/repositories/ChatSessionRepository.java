package com.haircutbooking.Haircut_Booking.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.haircutbooking.Haircut_Booking.domain.ChatSession;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    Optional<ChatSession> findBySessionId(String sessionId);

    Optional<ChatSession> findByExternalUserIdAndPlatformAndActiveTrue(String externalUserId, String platform);

    @Query("SELECT cs FROM ChatSession cs WHERE cs.externalUserId = :externalUserId AND cs.platform = :platform ORDER BY cs.lastInteraction DESC")
    List<ChatSession> findByExternalUserIdAndPlatformOrderByLastInteractionDesc(
            @Param("externalUserId") String externalUserId,
            @Param("platform") String platform);

    @Query("SELECT cs FROM ChatSession cs WHERE cs.pendingAppointmentId IS NOT NULL AND cs.active = true")
    List<ChatSession> findAllWithPendingAppointments();

    /**
     * Find active chat session by external user ID
     */
    Optional<ChatSession> findByExternalUserIdAndActiveTrue(String externalUserId);
}