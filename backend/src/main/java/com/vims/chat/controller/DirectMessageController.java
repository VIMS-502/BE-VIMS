package com.vims.chat.controller;

import com.vims.chat.dto.DirectMessage;
import com.vims.chat.service.DirectMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DirectMessageController {

    private final DirectMessageService directMessageService;

    @MessageMapping("/dm.send")
    public void sendDirectMessage(@Payload DirectMessage directMessage) {
        log.info("WebSocket: Sending DM from {} to {}", 
                directMessage.getSenderName(), directMessage.getReceiverName());
        directMessageService.sendDirectMessage(directMessage);
    }
}