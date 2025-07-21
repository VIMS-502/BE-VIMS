package com.vims.chat.repository;

import com.vims.chat.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    // 강의장 채팅 메시지 조회 (최신순)
    @Query("SELECT m FROM Message m WHERE m.roomId = :roomId AND m.messageType IN ('CHAT', 'ANNOUNCEMENT', 'SYSTEM') ORDER BY m.createdAt DESC")
    Page<Message> findLectureChatMessages(@Param("roomId") Long roomId, Pageable pageable);
    
    // DM 메시지 조회 (두 사용자 간, 최신순)
    @Query("SELECT m FROM Message m WHERE m.messageType = 'DM' AND " +
           "((m.senderId = :userId1 AND m.receiverId = :userId2) OR " +
           "(m.senderId = :userId2 AND m.receiverId = :userId1)) " +
           "ORDER BY m.createdAt DESC")
    Page<Message> findDirectMessages(@Param("userId1") Long userId1, 
                                   @Param("userId2") Long userId2, 
                                   Pageable pageable);
    
    // DM 메시지 조회 (두 사용자 간, 오래된 순 - 채팅창 표시용)
    @Query("SELECT m FROM Message m WHERE m.messageType = 'DM' AND " +
           "((m.senderId = :userId1 AND m.receiverId = :userId2) OR " +
           "(m.senderId = :userId2 AND m.receiverId = :userId1)) " +
           "ORDER BY m.createdAt ASC")
    Page<Message> findDirectMessagesAsc(@Param("userId1") Long userId1, 
                                      @Param("userId2") Long userId2, 
                                      Pageable pageable);
    
    // 최근 DM 상대방 목록 조회 (특정 사용자의)
    @Query("SELECT DISTINCT CASE " +
           "WHEN m.senderId = :userId THEN m.receiverId " +
           "ELSE m.senderId END as otherUserId, " +
           "MAX(m.createdAt) as lastMessageTime " +
           "FROM Message m WHERE m.messageType = 'DM' AND " +
           "(m.senderId = :userId OR m.receiverId = :userId) " +
           "GROUP BY otherUserId " +
           "ORDER BY lastMessageTime DESC")
    List<Object[]> findRecentDmPartners(@Param("userId") Long userId);
    
    // 읽지 않은 DM 개수 (간단한 구현 - 실제로는 읽음 상태 테이블이 별도로 필요)
    @Query("SELECT COUNT(m) FROM Message m WHERE m.messageType = 'DM' AND " +
           "m.receiverId = :userId AND m.createdAt > :lastReadTime")
    Long countUnreadDMs(@Param("userId") Long userId, @Param("lastReadTime") java.time.LocalDateTime lastReadTime);
    
    // 강의장 메시지 개수
    @Query("SELECT COUNT(m) FROM Message m WHERE m.roomId = :roomId AND m.messageType IN ('CHAT', 'ANNOUNCEMENT', 'SYSTEM')")
    Long countLectureChatMessages(@Param("roomId") Long roomId);
}