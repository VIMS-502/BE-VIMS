package com.vims.chat.service;

import com.vims.chat.dto.LectureChatMessage;
import com.vims.chat.dto.LectureJoinMessage;
import com.vims.lecture.service.LectureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LectureChatService {

    private final SimpMessagingTemplate messagingTemplate;
    private final LectureService lectureService;

    public void joinLecture(LectureJoinMessage joinMessage) {
        // 강의실 참여자 등록
        boolean joined = lectureService.joinLecture(joinMessage.getLectureId(), joinMessage.getUserId());
        
        if (joined) {
            LectureChatMessage chatMessage = new LectureChatMessage();
            chatMessage.setId(UUID.randomUUID().toString());
            chatMessage.setLectureId(joinMessage.getLectureId());
            chatMessage.setSenderId(joinMessage.getUserId());
            chatMessage.setSenderName(joinMessage.getUserName());
            chatMessage.setContent(joinMessage.getUserName() + "님이 강의실에 입장했습니다. " +
                    "(참여자 " + lectureService.getLectureParticipantCount(joinMessage.getLectureId()) + "명)");
            chatMessage.setType(LectureChatMessage.MessageType.JOIN);
            chatMessage.setTimestamp(LocalDateTime.now());
            
            // 강의실 전체에 브로드캐스트
            messagingTemplate.convertAndSend("/topic/lecture." + joinMessage.getLectureId(), chatMessage);
            
            log.info("User {} joined lecture {}", joinMessage.getUserName(), joinMessage.getLectureId());
        }
    }

    public void leaveLecture(LectureJoinMessage leaveMessage) {
        // 강의실 참여자 제거
        boolean left = lectureService.leaveLecture(leaveMessage.getLectureId(), leaveMessage.getUserId());
        
        if (left) {
            LectureChatMessage chatMessage = new LectureChatMessage();
            chatMessage.setId(UUID.randomUUID().toString());
            chatMessage.setLectureId(leaveMessage.getLectureId());
            chatMessage.setSenderId(leaveMessage.getUserId());
            chatMessage.setSenderName(leaveMessage.getUserName());
            chatMessage.setContent(leaveMessage.getUserName() + "님이 강의실에서 퇴장했습니다. " +
                    "(참여자 " + lectureService.getLectureParticipantCount(leaveMessage.getLectureId()) + "명)");
            chatMessage.setType(LectureChatMessage.MessageType.LEAVE);
            chatMessage.setTimestamp(LocalDateTime.now());
            
            // 강의실 전체에 브로드캐스트
            messagingTemplate.convertAndSend("/topic/lecture." + leaveMessage.getLectureId(), chatMessage);
            
            log.info("User {} left lecture {}", leaveMessage.getUserName(), leaveMessage.getLectureId());
        }
    }

    public void sendMessage(LectureChatMessage chatMessage) {
        // 사용자가 해당 강의실에 참여하고 있는지 확인
        if (!lectureService.isUserInLecture(chatMessage.getLectureId(), chatMessage.getSenderId())) {
            log.warn("User {} tried to send message to lecture {} but not a participant", 
                    chatMessage.getSenderId(), chatMessage.getLectureId());
            return;
        }
        
        chatMessage.setId(UUID.randomUUID().toString());
        chatMessage.setTimestamp(LocalDateTime.now());
        chatMessage.setType(LectureChatMessage.MessageType.CHAT);
        
        // 강의실 전체에 브로드캐스트
        messagingTemplate.convertAndSend("/topic/lecture." + chatMessage.getLectureId(), chatMessage);
        
        log.info("Lecture message sent from {} in lecture {}", 
                chatMessage.getSenderName(), chatMessage.getLectureId());
    }

    public void sendAnnouncement(LectureChatMessage announcement) {
        // 공지사항 권한 확인 (강사/TA만 가능)
        if (!isInstructorOrTA(announcement.getSenderId())) {
            log.warn("User {} tried to send announcement without permission in lecture {}", 
                    announcement.getSenderId(), announcement.getLectureId());
            return;
        }
        
        announcement.setId(UUID.randomUUID().toString());
        announcement.setTimestamp(LocalDateTime.now());
        announcement.setType(LectureChatMessage.MessageType.ANNOUNCEMENT);
        
        // 강의실 전체에 브로드캐스트 (공지사항)
        messagingTemplate.convertAndSend("/topic/lecture." + announcement.getLectureId(), announcement);
        
        log.info("Announcement sent from {} in lecture {}", 
                announcement.getSenderName(), announcement.getLectureId());
    }
    
    private boolean isInstructorOrTA(String userId) {
        // TODO: 실제 구현시 사용자 역할 확인 로직 추가
        // 현재는 임시로 true 반환 (개발 단계)
        return true;
    }

    public void sendSystemMessage(String lectureId, String content) {
        LectureChatMessage systemMessage = new LectureChatMessage();
        systemMessage.setId(UUID.randomUUID().toString());
        systemMessage.setLectureId(lectureId);
        systemMessage.setSenderId("SYSTEM");
        systemMessage.setSenderName("시스템");
        systemMessage.setContent(content);
        systemMessage.setType(LectureChatMessage.MessageType.SYSTEM);
        systemMessage.setTimestamp(LocalDateTime.now());
        
        messagingTemplate.convertAndSend("/topic/lecture." + lectureId, systemMessage);
        
        log.info("System message sent to lecture {}: {}", lectureId, content);
    }
}