package com.haircutbooking.Haircut_Booking.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.haircutbooking.Haircut_Booking.domain.ChatMessage;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByChatSessionIdOrderByTimestampAsc(Long chatSessionId);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chatSession.sessionId = :sessionId ORDER BY cm.timestamp ASC")
    List<ChatMessage> findBySessionIdOrderByTimestampAsc(@Param("sessionId") String sessionId);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chatSession.externalUserId = :externalUserId " +
            "AND cm.chatSession.platform = :platform ORDER BY cm.timestamp DESC")
    List<ChatMessage> findRecentMessagesByExternalUserIdAndPlatform(
            @Param("externalUserId") String externalUserId,
            @Param("platform") String platform);
}