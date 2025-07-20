package com.vims.chat.controller;

import com.vims.chat.dto.ChatMessage;
import com.vims.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        log.info("Sending message from {} to room {}", chatMessage.getSenderId(), chatMessage.getRoomId());
        chatService.sendMessage(chatMessage);
    }

    @MessageMapping("/chat.join")
    public void addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        log.info("User {} joining chat room {}", chatMessage.getSenderId(), chatMessage.getRoomId());
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSenderId());
        headerAccessor.getSessionAttributes().put("roomId", chatMessage.getRoomId());
        chatService.addUser(chatMessage);
    }
}