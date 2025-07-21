package com.vims.chat.controller;

import com.vims.chat.dto.LectureChatMessage;
import com.vims.chat.dto.LectureJoinMessage;
import com.vims.chat.entity.Message;
import com.vims.chat.service.LectureChatService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Controller
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class LectureChatController {

    private static final Logger log = LoggerFactory.getLogger(LectureChatController.class);
    private final LectureChatService lectureChatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/lecture.join")
    public void joinLecture(@Payload LectureJoinMessage joinMessage, 
                           SimpMessageHeaderAccessor headerAccessor) {
        log.info("🔥 WebSocket: User {} (ID: {}) joining lecture {}", 
                joinMessage.getUserName(), joinMessage.getUserId(), joinMessage.getLectureId());
        log.info("🔥 Session ID: {}", headerAccessor.getSessionId());
        log.info("🔥 Session attributes: {}", headerAccessor.getSessionAttributes());
        log.info("🔥 Raw message payload: {}", joinMessage);
        
        // 세션에 사용자 정보 저장 (자동 퇴장을 위해)
        headerAccessor.getSessionAttributes().put("userId", joinMessage.getUserId());
        headerAccessor.getSessionAttributes().put("userName", joinMessage.getUserName());
        headerAccessor.getSessionAttributes().put("lectureId", joinMessage.getLectureId());
        headerAccessor.getSessionAttributes().put("userRole", joinMessage.getUserRole());
        
        lectureChatService.joinLecture(joinMessage);
        
        // 강의장 입장 시 최근 메시지 히스토리 전송
        log.info("🔍 Retrieving history for lecture {} for user {}", joinMessage.getLectureId(), joinMessage.getUserId());
        List<Message> recentMessages = lectureChatService.getLectureChatHistory(joinMessage.getLectureId(), 0, 50);
        log.info("📜 Retrieved {} messages from database for lecture {}", recentMessages.size(), joinMessage.getLectureId());
        
        if (!recentMessages.isEmpty()) {
            // WebSocket을 통한 히스토리 전송 (단일 방법)
            log.info("📤 Sending {} message history to user {} via WebSocket", recentMessages.size(), joinMessage.getUserId());
            
            try {
                messagingTemplate.convertAndSendToUser(
                    joinMessage.getUserId(),
                    "/queue/lecture-history",
                    recentMessages
                );
                log.info("✅ Sent {} message history to user {} successfully", 
                        recentMessages.size(), joinMessage.getUserId());
            } catch (Exception e) {
                log.error("❌ Failed to send history to user {}: {}", joinMessage.getUserId(), e.getMessage());
            }
            
        } else {
            log.warn("⚠️ No messages found for lecture {}", joinMessage.getLectureId());
        }
    }

    @MessageMapping("/lecture.leave")
    public void leaveLecture(@Payload LectureJoinMessage leaveMessage) {
        log.info("WebSocket: User {} leaving lecture {}", leaveMessage.getUserName(), leaveMessage.getLectureId());
        lectureChatService.leaveLecture(leaveMessage);
    }

    @MessageMapping("/lecture.send")
    public void sendLectureMessage(@Payload LectureChatMessage chatMessage) {
        log.info("WebSocket: Sending lecture message from {} to lecture {}", 
                chatMessage.getSenderName(), chatMessage.getLectureId());
        
        // 메시지 타입에 따른 처리 (일반 채팅 또는 공지사항)
        if (chatMessage.getType() == LectureChatMessage.MessageType.ANNOUNCEMENT) {
            lectureChatService.sendAnnouncement(chatMessage);
        } else {
            lectureChatService.sendMessage(chatMessage);
        }
    }

    // 강의장 채팅 히스토리 조회 API
    @GetMapping("/lecture/history")
    public ResponseEntity<List<Message>> getLectureChatHistory(
            @RequestParam String lectureId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        List<Message> messages = lectureChatService.getLectureChatHistory(lectureId, page, size);
        return ResponseEntity.ok(messages);
    }

    // 강의장 메시지 개수 조회 API
    @GetMapping("/lecture/message-count")
    public ResponseEntity<Long> getLectureChatMessageCount(@RequestParam String lectureId) {
        Long count = lectureChatService.getLectureChatMessageCount(lectureId);
        return ResponseEntity.ok(count);
    }
}