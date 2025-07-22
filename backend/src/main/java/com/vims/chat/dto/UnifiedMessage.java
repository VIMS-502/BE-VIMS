package com.vims.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor 
@AllArgsConstructor
public class UnifiedMessage {
    private String id;
    private MessageCategory category;
    private MessageType type;
    private Object payload;
    private LocalDateTime timestamp;
    
    public enum MessageCategory {
        ROOM,           // 방 관련 메시지 (실시간 + 히스토리)
        NOTIFICATION    // 개인 알림
    }
    
    public enum MessageType {
        // Room Messages
        REALTIME_MESSAGE,   // 실시간 채팅
        HISTORY_SYNC,       // 히스토리 동기화
        USER_JOIN,          // 사용자 입장
        USER_LEAVE,         // 사용자 퇴장
        ANNOUNCEMENT,       // 공지사항
        
        // Notifications
        DM_RECEIVED,        // DM 수신 알림
        MENTION,            // 멘션 알림
        ROOM_INVITE         // 방 초대
    }
    
    // 정적 팩토리 메서드들
    public static UnifiedMessage realtimeMessage(Object messageData) {
        return new UnifiedMessage(
            java.util.UUID.randomUUID().toString(),
            MessageCategory.ROOM,
            MessageType.REALTIME_MESSAGE,
            messageData,
            LocalDateTime.now()
        );
    }
    
    public static UnifiedMessage historySync(List<?> historyData) {
        return new UnifiedMessage(
            java.util.UUID.randomUUID().toString(),
            MessageCategory.ROOM,
            MessageType.HISTORY_SYNC,
            historyData,
            LocalDateTime.now()
        );
    }
    
    public static UnifiedMessage userJoin(Object joinData) {
        return new UnifiedMessage(
            java.util.UUID.randomUUID().toString(),
            MessageCategory.ROOM,
            MessageType.USER_JOIN,
            joinData,
            LocalDateTime.now()
        );
    }
    
    public static UnifiedMessage userLeave(Object leaveData) {
        return new UnifiedMessage(
            java.util.UUID.randomUUID().toString(),
            MessageCategory.ROOM,
            MessageType.USER_LEAVE,
            leaveData,
            LocalDateTime.now()
        );
    }
    
    public static UnifiedMessage announcement(Object announcementData) {
        return new UnifiedMessage(
            java.util.UUID.randomUUID().toString(),
            MessageCategory.ROOM,
            MessageType.ANNOUNCEMENT,
            announcementData,
            LocalDateTime.now()
        );
    }
    
    public static UnifiedMessage dmNotification(Object notificationData) {
        return new UnifiedMessage(
            java.util.UUID.randomUUID().toString(),
            MessageCategory.NOTIFICATION,
            MessageType.DM_RECEIVED,
            notificationData,
            LocalDateTime.now()
        );
    }
}