package com.vims.chat.controller;

import com.vims.chat.dto.RoomChatMessage;
import com.vims.chat.dto.RoomJoinMessage;
import com.vims.chat.entity.Message;
import com.vims.chat.service.RoomChatService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class RoomChatController {

    private static final Logger log = LoggerFactory.getLogger(RoomChatController.class);
    private final RoomChatService roomChatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/room.join")
    public void joinRoom(@Payload RoomJoinMessage joinMessage, 
                        SimpMessageHeaderAccessor headerAccessor) {
        log.info("🔥 WebSocket: User {} (ID: {}) joining room {}", 
                joinMessage.getUserName(), joinMessage.getUserId(), joinMessage.getRoomCode());
        log.info("🔥 Session ID: {}", headerAccessor.getSessionId());
        log.info("🔥 Session attributes: {}", headerAccessor.getSessionAttributes());
        log.info("🔥 Raw message payload: {}", joinMessage);
        
        // 세션에 사용자 정보 저장 (자동 퇴장을 위해)
        headerAccessor.getSessionAttributes().put("userId", joinMessage.getUserId());
        headerAccessor.getSessionAttributes().put("userName", joinMessage.getUserName());
        headerAccessor.getSessionAttributes().put("roomCode", joinMessage.getRoomCode());
        headerAccessor.getSessionAttributes().put("userRole", joinMessage.getUserRole());
        
        roomChatService.joinRoom(joinMessage);
        
        // 방 입장 시 최근 메시지 히스토리 전송
        log.info("🔍 Retrieving history for room {} for user {}", joinMessage.getRoomCode(), joinMessage.getUserId());
        List<Message> recentMessages = roomChatService.getRoomChatHistory(joinMessage.getRoomCode(), 0, 50);
        log.info("📜 Retrieved {} messages from database for room {}", recentMessages.size(), joinMessage.getRoomCode());
        
        // 통합 메시지로 히스토리 전송 (방 전체에)
        if (!recentMessages.isEmpty()) {
            log.info("📤 Sending {} message history to room via unified message", recentMessages.size());
            
            try {
                com.vims.chat.dto.UnifiedMessage historyMessage = 
                    com.vims.chat.dto.UnifiedMessage.historySync(recentMessages);
                
                // 방 전체에 히스토리 전송 (새로 입장한 사용자만 처리하도록 클라이언트에서 필터링)
                messagingTemplate.convertAndSend(
                    "/room/" + joinMessage.getRoomCode(), 
                    historyMessage
                );
                
                log.info("✅ Sent unified history message to room {}", joinMessage.getRoomCode());
            } catch (Exception e) {
                log.error("❌ Failed to send unified history: {}", e.getMessage());
            }
        } else {
            log.warn("⚠️ No messages found for room {}", joinMessage.getRoomCode());
        }
    }

    @MessageMapping("/room.leave")
    public void leaveRoom(@Payload RoomJoinMessage leaveMessage) {
        log.info("WebSocket: User {} leaving room {}", leaveMessage.getUserName(), leaveMessage.getRoomCode());
        roomChatService.leaveRoom(leaveMessage);
    }

    @MessageMapping("/room.send")
    public void sendRoomMessage(@Payload RoomChatMessage chatMessage) {
        log.info("WebSocket: Sending room message from {} to room {}", 
                chatMessage.getSenderName(), chatMessage.getRoomCode());
        
        // 일반 채팅 메시지 처리
        roomChatService.sendMessage(chatMessage);
    }

    // 방 채팅 히스토리 조회 API
    @GetMapping("/room/history")
    public ResponseEntity<List<Message>> getRoomChatHistory(
            @RequestParam String roomCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        List<Message> messages = roomChatService.getRoomChatHistory(roomCode, page, size);
        return ResponseEntity.ok(messages);
    }

    // 방 메시지 개수 조회 API
    @GetMapping("/room/message-count")
    public ResponseEntity<Long> getRoomChatMessageCount(@RequestParam String roomCode) {
        Long count = roomChatService.getRoomChatMessageCount(roomCode);
        return ResponseEntity.ok(count);
    }
    
}