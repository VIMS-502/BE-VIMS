package com.vims.chat.service;

import com.vims.chat.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendMessage(ChatMessage chatMessage) {
        chatMessage.setId(UUID.randomUUID().toString());
        chatMessage.setTimestamp(LocalDateTime.now());
        chatMessage.setType(ChatMessage.MessageType.CHAT);
        
        messagingTemplate.convertAndSend("/topic/chat." + chatMessage.getRoomId(), chatMessage);
    }

    public void addUser(ChatMessage chatMessage) {
        chatMessage.setId(UUID.randomUUID().toString());
        chatMessage.setTimestamp(LocalDateTime.now());
        chatMessage.setType(ChatMessage.MessageType.JOIN);
        chatMessage.setContent(chatMessage.getSenderName() + "님이 입장했습니다.");
        
        messagingTemplate.convertAndSend("/topic/chat." + chatMessage.getRoomId(), chatMessage);
    }

    public void removeUser(ChatMessage chatMessage) {
        chatMessage.setId(UUID.randomUUID().toString());
        chatMessage.setTimestamp(LocalDateTime.now());
        chatMessage.setType(ChatMessage.MessageType.LEAVE);
        chatMessage.setContent(chatMessage.getSenderName() + "님이 퇴장했습니다.");
        
        messagingTemplate.convertAndSend("/topic/chat." + chatMessage.getRoomId(), chatMessage);
    }
}