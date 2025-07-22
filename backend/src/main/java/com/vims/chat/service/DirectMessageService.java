package com.vims.chat.service;

import com.vims.chat.dto.DirectMessage;
import com.vims.chat.entity.Message;
import com.vims.chat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DirectMessageService {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageRepository messageRepository;

    @Transactional
    public void sendDirectMessage(DirectMessage directMessage) {
        directMessage.setId(UUID.randomUUID().toString());
        directMessage.setTimestamp(LocalDateTime.now());
        directMessage.setType(DirectMessage.MessageType.TEXT);
        
        // DB에 메시지 저장 (DM용)
        Message messageEntity = new Message();
        messageEntity.setSenderId(Long.parseLong(directMessage.getSenderId()));
        messageEntity.setReceiverId(Long.parseLong(directMessage.getReceiverId()));
        messageEntity.setMessageType(Message.MessageType.DM);
        messageEntity.setContent(directMessage.getContent());
        messageRepository.save(messageEntity);
        
        String roomId = directMessage.generateRoomId();
        
        // DM 방에 메시지 전송 (양쪽 사용자 모두 수신)
        messagingTemplate.convertAndSend("/room/dm." + roomId, directMessage);
        
        // 수신자에게 통합 알림 전송
        com.vims.chat.dto.UnifiedMessage dmNotification = 
            com.vims.chat.dto.UnifiedMessage.dmNotification(createNotification(directMessage));
        messagingTemplate.convertAndSendToUser(
            directMessage.getReceiverId(), 
            "/queue/notifications", 
            dmNotification
        );
        
        log.info("Direct message saved to DB and sent from {} to {} in room {}", 
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

    // DM 히스토리 조회 (페이징)
    public List<Message> getDirectMessageHistory(String userId1, String userId2, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messagePage = messageRepository.findDirectMessagesAsc(
            Long.parseLong(userId1), 
            Long.parseLong(userId2), 
            pageable
        );
        return messagePage.getContent();
    }

    // 최근 DM 상대방 목록 조회
    public List<Object[]> getRecentDmPartners(String userId) {
        return messageRepository.findRecentDmPartners(Long.parseLong(userId));
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