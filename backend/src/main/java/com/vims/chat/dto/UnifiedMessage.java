package com.vims.chat.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor 
public class UnifiedMessage {
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
        
        // Notifications
        DM_RECEIVED,        // DM 수신 알림
        MENTION,            // 멘션 알림
        ROOM_INVITE         // 방 초대
    }
    
    // 정적 팩토리 메서드들
    public static UnifiedMessage realtimeMessage(Object messageData) {
        UnifiedMessage message = new UnifiedMessage();
        message.setCategory(MessageCategory.ROOM);
        message.setType(MessageType.REALTIME_MESSAGE);
        message.setPayload(messageData);
        message.setTimestamp(LocalDateTime.now());
        return message;
    }
    
    public static UnifiedMessage historySync(List<?> historyData) {
        UnifiedMessage message = new UnifiedMessage();
        message.setCategory(MessageCategory.ROOM);
        message.setType(MessageType.HISTORY_SYNC);
        message.setPayload(historyData);
        message.setTimestamp(LocalDateTime.now());
        return message;
    }
    
    public static UnifiedMessage userJoin(Object joinData) {
        UnifiedMessage message = new UnifiedMessage();
        message.setCategory(MessageCategory.ROOM);
        message.setType(MessageType.USER_JOIN);
        message.setPayload(joinData);
        message.setTimestamp(LocalDateTime.now());
        return message;
    }
    
    public static UnifiedMessage userLeave(Object leaveData) {
        UnifiedMessage message = new UnifiedMessage();
        message.setCategory(MessageCategory.ROOM);
        message.setType(MessageType.USER_LEAVE);
        message.setPayload(leaveData);
        message.setTimestamp(LocalDateTime.now());
        return message;
    }
    
    public static UnifiedMessage dmNotification(Object notificationData) {
        UnifiedMessage message = new UnifiedMessage();
        message.setCategory(MessageCategory.NOTIFICATION);
        message.setType(MessageType.DM_RECEIVED);
        message.setPayload(notificationData);
        message.setTimestamp(LocalDateTime.now());
        return message;
    }
}