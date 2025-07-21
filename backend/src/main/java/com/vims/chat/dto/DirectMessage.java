package com.vims.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DirectMessage {
    private String id;
    private String senderId;
    private String senderName;
    private String receiverId;
    private String receiverName;
    private String content;
    private MessageType type;
    private LocalDateTime timestamp;
    
    public enum MessageType {
        TEXT,
        FILE,
        SYSTEM
    }
    
    public String generateRoomId() {
        // 항상 같은 순서로 정렬하여 동일한 roomId 생성
        if (senderId.compareTo(receiverId) < 0) {
            return "dm_" + senderId + "_" + receiverId;
        } else {
            return "dm_" + receiverId + "_" + senderId;
        }
    }
}