package com.vims.webrtc.config;

import com.vims.chat.dto.LectureJoinMessage;
import com.vims.chat.service.LectureChatService;
import com.vims.lecture.service.LectureService;
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

    private final LectureChatService lectureChatService;
    private final LectureService lectureService;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        
        String userId = (String) headerAccessor.getSessionAttributes().get("userId");
        String userName = (String) headerAccessor.getSessionAttributes().get("userName");
        String lectureId = (String) headerAccessor.getSessionAttributes().get("lectureId");
        String userRole = (String) headerAccessor.getSessionAttributes().get("userRole");
        
        if (userId != null) {
            log.info("User {} disconnected from session", userName != null ? userName : userId);
            
            // 강의실에서 자동 퇴장 처리
            if (lectureId != null) {
                LectureJoinMessage leaveMessage = new LectureJoinMessage();
                leaveMessage.setLectureId(lectureId);
                leaveMessage.setUserId(userId);
                leaveMessage.setUserName(userName != null ? userName : userId);
                leaveMessage.setUserRole(userRole != null ? userRole : "STUDENT");
                
                lectureChatService.leaveLecture(leaveMessage);
            } else {
                // 세션 정리 (사용자가 어떤 강의실에 있었는지 확인 후 정리)
                lectureService.cleanupUserSession(userId);
            }
        }
        
        // 기존 레거시 세션 정리 (하위 호환성)
        String legacyUsername = (String) headerAccessor.getSessionAttributes().get("username");
        String legacyRoomId = (String) headerAccessor.getSessionAttributes().get("roomId");
        
        if (legacyUsername != null && legacyRoomId != null) {
            log.info("Legacy session cleanup for user {} in room {}", legacyUsername, legacyRoomId);
            // 필요시 기존 ChatService 호출하여 정리
        }
    }
}