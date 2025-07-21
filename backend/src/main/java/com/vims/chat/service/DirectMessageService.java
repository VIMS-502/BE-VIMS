package com.vims.chat.service;

import com.vims.chat.dto.DirectMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DirectMessageService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendDirectMessage(DirectMessage directMessage) {
        directMessage.setId(UUID.randomUUID().toString());
        directMessage.setTimestamp(LocalDateTime.now());
        directMessage.setType(DirectMessage.MessageType.TEXT);
        
        String roomId = directMessage.generateRoomId();
        
        // DM 방에 메시지 전송 (양쪽 사용자 모두 수신)
        messagingTemplate.convertAndSend("/topic/dm." + roomId, directMessage);
        
        // 수신자에게 개인 알림 전송
        messagingTemplate.convertAndSendToUser(
            directMessage.getReceiverId(), 
            "/queue/dm-notification", 
            createNotification(directMessage)
        );
        
        log.info("Direct message sent from {} to {} in room {}", 
                directMessage.getSenderName(), 
                directMessage.getReceiverName(), 
                roomId);
    }


    private DirectMessageNotification createNotification(DirectMessage directMessage) {
        return new DirectMessageNotification(
            directMessage.getId(),
            directMessage.getSenderId(),
            directMessage.getSenderName(),
            "새로운 메시지가 도착했습니다: " + truncateContent(directMessage.getContent()),
            directMessage.getTimestamp()
        );
    }

    private String truncateContent(String content) {
        if (content.length() > 50) {
            return content.substring(0, 47) + "...";
        }
        return content;
    }

    // 내부 클래스로 알림 DTO 정의
    public static class DirectMessageNotification {
        private String messageId;
        private String senderId;
        private String senderName;
        private String preview;
        private LocalDateTime timestamp;

        public DirectMessageNotification(String messageId, String senderId, String senderName, 
                                       String preview, LocalDateTime timestamp) {
            this.messageId = messageId;
            this.senderId = senderId;
            this.senderName = senderName;
            this.preview = preview;
            this.timestamp = timestamp;
        }

        // Getters
        public String getMessageId() { return messageId; }
        public String getSenderId() { return senderId; }
        public String getSenderName() { return senderName; }
        public String getPreview() { return preview; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}