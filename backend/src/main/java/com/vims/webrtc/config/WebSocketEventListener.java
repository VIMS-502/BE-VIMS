package com.vims.webrtc.config;

import com.vims.chat.dto.ChatMessage;
import com.vims.chat.service.ChatService;
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

    private final ChatService chatService;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        String roomId = (String) headerAccessor.getSessionAttributes().get("roomId");
        
        if (username != null && roomId != null) {
            log.info("User {} disconnected from room {}", username, roomId);
            
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setSenderId(username);
            chatMessage.setSenderName(username);
            chatMessage.setRoomId(roomId);
            
            chatService.removeUser(chatMessage);
        }
    }
}