package com.vims.chat.controller;

import com.vims.chat.dto.LectureChatMessage;
import com.vims.chat.dto.LectureJoinMessage;
import com.vims.chat.service.LectureChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class LectureChatController {

    private final LectureChatService lectureChatService;

    @MessageMapping("/lecture.join")
    public void joinLecture(@Payload LectureJoinMessage joinMessage, 
                           SimpMessageHeaderAccessor headerAccessor) {
        log.info("WebSocket: User {} joining lecture {}", joinMessage.getUserName(), joinMessage.getLectureId());
        
        // 세션에 사용자 정보 저장 (자동 퇴장을 위해)
        headerAccessor.getSessionAttributes().put("userId", joinMessage.getUserId());
        headerAccessor.getSessionAttributes().put("userName", joinMessage.getUserName());
        headerAccessor.getSessionAttributes().put("lectureId", joinMessage.getLectureId());
        headerAccessor.getSessionAttributes().put("userRole", joinMessage.getUserRole());
        
        lectureChatService.joinLecture(joinMessage);
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
}