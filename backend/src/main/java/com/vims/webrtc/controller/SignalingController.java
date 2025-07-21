package com.vims.webrtc.controller;

import com.vims.webrtc.dto.SignalingMessage;
import com.vims.webrtc.service.SignalingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SignalingController {

    private final SignalingService signalingService;

    @MessageMapping("/webrtc.join")
    public void joinRoom(@Payload SignalingMessage message, SimpMessageHeaderAccessor headerAccessor) {
        log.info("User {} joining room {}", message.getFrom(), message.getRoomId());
        signalingService.joinRoom(message, headerAccessor.getSessionId());
    }

    @MessageMapping("/webrtc.leave")
    public void leaveRoom(@Payload SignalingMessage message, SimpMessageHeaderAccessor headerAccessor) {
        log.info("User {} leaving room {}", message.getFrom(), message.getRoomId());
        signalingService.leaveRoom(message, headerAccessor.getSessionId());
    }

    @MessageMapping("/webrtc.offer")
    public void sendOffer(@Payload SignalingMessage message) {
        log.info("Sending offer from {} to {}", message.getFrom(), message.getTo());
        signalingService.sendOffer(message);
    }

    @MessageMapping("/webrtc.answer")
    public void sendAnswer(@Payload SignalingMessage message) {
        log.info("Sending answer from {} to {}", message.getFrom(), message.getTo());
        signalingService.sendAnswer(message);
    }

    @MessageMapping("/webrtc.ice")
    public void sendIceCandidate(@Payload SignalingMessage message) {
        log.info("Sending ICE candidate from {} to {}", message.getFrom(), message.getTo());
        signalingService.sendIceCandidate(message);
    }
}