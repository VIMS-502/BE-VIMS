package com.vims.webrtc.config;

import com.vims.chat.dto.RoomJoinMessage;
import com.vims.chat.service.RoomChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final RoomChatService roomChatService;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        
        Integer userId = (Integer) headerAccessor.getSessionAttributes().get("userId");
        String userName = (String) headerAccessor.getSessionAttributes().get("userName");
        String roomCode = (String) headerAccessor.getSessionAttributes().get("roomCode");
        String userRole = (String) headerAccessor.getSessionAttributes().get("userRole");
        
        if (userId != null) {
            log.info("User {} disconnected from session", userName != null ? userName : userId.toString());
            
            // 방에서 자동 퇴장 처리
            if (roomCode != null) {
                RoomJoinMessage leaveMessage = new RoomJoinMessage();
                leaveMessage.setRoomCode(roomCode);
                leaveMessage.setUserId(userId);
                leaveMessage.setUserName(userName != null ? userName : userId.toString());
                leaveMessage.setUserRole(userRole != null ? userRole : "STUDENT");
                
                roomChatService.leaveRoom(leaveMessage);
            }
        }
    }
}