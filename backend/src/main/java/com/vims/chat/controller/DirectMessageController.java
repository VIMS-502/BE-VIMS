package com.vims.chat.controller;

import com.vims.chat.dto.DirectMessage;
import com.vims.chat.entity.Message;
import com.vims.chat.service.DirectMessageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class DirectMessageController {

    private static final Logger log = LoggerFactory.getLogger(DirectMessageController.class);
    private final DirectMessageService directMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/dm.send")
    public void sendDirectMessage(@Payload DirectMessage directMessage) {
        log.info("WebSocket: Sending DM from {} to {}", 
                directMessage.getSenderName(), directMessage.getReceiverName());
        directMessageService.sendDirectMessage(directMessage);
    }

    // DM방 열기 (구독과 동시에 히스토리 로드)
    @MessageMapping("/dm.join")
    public void joinDmRoom(@Payload Map<String, String> joinRequest) {
        String userId1 = joinRequest.get("userId1");
        String userId2 = joinRequest.get("userId2");
        String requesterId = joinRequest.get("requesterId");
        
        log.info("WebSocket: User {} joining DM room with {}", requesterId, 
                userId1.equals(requesterId) ? userId2 : userId1);
        
        // DM 히스토리 로드하여 요청자에게 전송
        List<Message> dmHistory = directMessageService.getDirectMessageHistory(Integer.parseInt(userId1), Integer.parseInt(userId2), 0, 50);
        
        if (!dmHistory.isEmpty()) {
            // 개별 사용자에게 DM 히스토리 전송
            messagingTemplate.convertAndSendToUser(
                requesterId,
                "/queue/dm-history",
                dmHistory
            );
            log.info("DM history loaded for user {}: {} messages", requesterId, dmHistory.size());
        }
    }

    // DM 히스토리 조회 API
    @GetMapping("/dm/history")
    public ResponseEntity<List<Message>> getDirectMessageHistory(
            @RequestParam Integer userId1,
            @RequestParam Integer userId2,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        List<Message> messages = directMessageService.getDirectMessageHistory(userId1, userId2, page, size);
        return ResponseEntity.ok(messages);
    }

    // 최근 DM 상대방 목록 조회 API
    @GetMapping("/dm/recent-partners")
    public ResponseEntity<List<Object[]>> getRecentDmPartners(@RequestParam Integer userId) {
        List<Object[]> partners = directMessageService.getRecentDmPartners(userId);
        return ResponseEntity.ok(partners);
    }
}