package com.vims.chat.service;

import com.vims.chat.dto.LectureChatMessage;
import com.vims.chat.dto.LectureJoinMessage;
import com.vims.chat.entity.Message;
import com.vims.chat.repository.MessageRepository;
import com.vims.lecture.service.LectureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LectureChatService {

    private final SimpMessagingTemplate messagingTemplate;
    private final LectureService lectureService;
    private final MessageRepository messageRepository;

    @Transactional
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
            
            // DB에 시스템 메시지 저장 (lectureId를 해시코드로 변환)
            Message messageEntity = new Message(
                (long) joinMessage.getLectureId().hashCode(),
                Long.parseLong(joinMessage.getUserId()),
                Message.MessageType.SYSTEM,
                chatMessage.getContent()
            );
            messageRepository.save(messageEntity);
            
            // 통합 메시지로 입장 알림 전송
            com.vims.chat.dto.UnifiedMessage joinMsg = 
                com.vims.chat.dto.UnifiedMessage.userJoin(chatMessage);
            messagingTemplate.convertAndSend("/room/lecture." + joinMessage.getLectureId(), joinMsg);
            
            log.info("User {} joined lecture {}", joinMessage.getUserName(), joinMessage.getLectureId());
        }
    }

    @Transactional
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
            
            // DB에 시스템 메시지 저장 (lectureId를 해시코드로 변환)
            Message messageEntity = new Message(
                (long) leaveMessage.getLectureId().hashCode(),
                Long.parseLong(leaveMessage.getUserId()),
                Message.MessageType.SYSTEM,
                chatMessage.getContent()
            );
            messageRepository.save(messageEntity);
            
            // 통합 메시지로 퇴장 알림 전송
            com.vims.chat.dto.UnifiedMessage leaveMsg = 
                com.vims.chat.dto.UnifiedMessage.userLeave(chatMessage);
            messagingTemplate.convertAndSend("/room/lecture." + leaveMessage.getLectureId(), leaveMsg);
            
            log.info("User {} left lecture {}", leaveMessage.getUserName(), leaveMessage.getLectureId());
        }
    }

    @Transactional
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
        
        // DB에 채팅 메시지 저장 (lectureId를 해시코드로 변환)
        Message messageEntity = new Message(
            (long) chatMessage.getLectureId().hashCode(),
            Long.parseLong(chatMessage.getSenderId()),
            Message.MessageType.CHAT,
            chatMessage.getContent()
        );
        messageRepository.save(messageEntity);
        
        // 통합 메시지로 실시간 채팅 전송
        com.vims.chat.dto.UnifiedMessage realtimeMsg = 
            com.vims.chat.dto.UnifiedMessage.realtimeMessage(chatMessage);
        messagingTemplate.convertAndSend("/room/lecture." + chatMessage.getLectureId(), realtimeMsg);
        
        log.info("Lecture message sent from {} in lecture {}", 
                chatMessage.getSenderName(), chatMessage.getLectureId());
    }

    @Transactional
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
        
        // DB에 공지사항 저장 (lectureId를 해시코드로 변환)
        Message messageEntity = new Message(
            (long) announcement.getLectureId().hashCode(),
            Long.parseLong(announcement.getSenderId()),
            Message.MessageType.ANNOUNCEMENT,
            announcement.getContent()
        );
        messageRepository.save(messageEntity);
        
        // 통합 메시지로 공지사항 전송
        com.vims.chat.dto.UnifiedMessage announcementMsg = 
            com.vims.chat.dto.UnifiedMessage.announcement(announcement);
        messagingTemplate.convertAndSend("/room/lecture." + announcement.getLectureId(), announcementMsg);
        
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

    // 강의장 채팅 히스토리 조회 (페이징)
    public List<Message> getLectureChatHistory(String lectureId, int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            // lectureId를 roomId로 사용 (문자열을 해시코드로 변환하여 Long으로 사용)
            Long roomId = (long) lectureId.hashCode();
            
            log.info("Searching for lecture history: lectureId={}, roomId={}, page={}, size={}", 
                    lectureId, roomId, page, size);
            
            Page<Message> messagePage = messageRepository.findLectureChatMessages(roomId, pageable);
            
            log.info("Found {} messages for lecture {}", messagePage.getContent().size(), lectureId);
            
            return messagePage.getContent();
        } catch (Exception e) {
            log.error("Error retrieving lecture chat history for lectureId: {}", lectureId, e);
            return List.of(); // 빈 리스트 반환
        }
    }

    // 강의장 메시지 개수 조회
    public Long getLectureChatMessageCount(String lectureId) {
        Long roomId = (long) lectureId.hashCode();
        return messageRepository.countLectureChatMessages(roomId);
    }
}