package com.vims.chat.service;

import com.vims.chat.dto.RoomChatMessage;
import com.vims.chat.dto.RoomJoinMessage;
import com.vims.chat.entity.Message;
import com.vims.room.entity.Room;
import com.vims.chat.repository.MessageRepository;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomChatService {

    private final SimpMessagingTemplate messagingTemplate;
    private final com.vims.room.service.RoomService roomService;
    private final MessageRepository messageRepository;
    
    // 방별 참여자 관리 (room_code -> Set<userId>)
    private final Map<String, Map<Integer, String>> roomParticipants = new ConcurrentHashMap<>();

    @Transactional
    public void joinRoom(RoomJoinMessage joinMessage) {
        // 방이 존재하는지 확인
        Room room = roomService.findByRoomCode(joinMessage.getRoomCode())
                .orElse(null);
        
        if (room == null) {
            log.warn("Room not found with code: {}", joinMessage.getRoomCode());
            return;
        }
        
        // 참여자 등록
        roomParticipants.computeIfAbsent(joinMessage.getRoomCode(), k -> new ConcurrentHashMap<>())
                .put(joinMessage.getUserId(), joinMessage.getUserName());
        
        RoomChatMessage chatMessage = new RoomChatMessage();
        chatMessage.setRoomCode(joinMessage.getRoomCode());
        chatMessage.setSenderId(joinMessage.getUserId());
        chatMessage.setSenderName(joinMessage.getUserName());
        chatMessage.setContent(joinMessage.getUserName() + "님이 방에 입장했습니다. " +
                "(참여자 " + getParticipantCount(joinMessage.getRoomCode()) + "명)");
        chatMessage.setType(RoomChatMessage.MessageType.JOIN);
        chatMessage.setTimestamp(LocalDateTime.now());
        
        // DB에 입장 메시지 저장
        Message messageEntity = new Message(
            room.getId(),
            joinMessage.getUserId().longValue(),
            Message.MessageType.CHAT,
            chatMessage.getContent()
        );
        messageRepository.save(messageEntity);
        
        // 통합 메시지로 입장 알림 전송
        com.vims.chat.dto.UnifiedMessage joinMsg = 
            com.vims.chat.dto.UnifiedMessage.userJoin(chatMessage);
        messagingTemplate.convertAndSend("/room/" + joinMessage.getRoomCode(), joinMsg);
        
        log.info("User {} joined room {}", joinMessage.getUserName(), joinMessage.getRoomCode());
    }

    @Transactional
    public void leaveRoom(RoomJoinMessage leaveMessage) {
        // 방이 존재하는지 확인
        Room room = roomService.findByRoomCode(leaveMessage.getRoomCode())
                .orElse(null);
        
        if (room == null) {
            log.warn("Room not found with code: {}", leaveMessage.getRoomCode());
            return;
        }
        
        // 참여자 제거
        Map<Integer, String> participants = roomParticipants.get(leaveMessage.getRoomCode());
        if (participants != null) {
            participants.remove(leaveMessage.getUserId());
            if (participants.isEmpty()) {
                roomParticipants.remove(leaveMessage.getRoomCode());
            }
        }
        
        RoomChatMessage chatMessage = new RoomChatMessage();
        chatMessage.setRoomCode(leaveMessage.getRoomCode());
        chatMessage.setSenderId(leaveMessage.getUserId());
        chatMessage.setSenderName(leaveMessage.getUserName());
        chatMessage.setContent(leaveMessage.getUserName() + "님이 방에서 퇴장했습니다. " +
                "(참여자 " + getParticipantCount(leaveMessage.getRoomCode()) + "명)");
        chatMessage.setType(RoomChatMessage.MessageType.LEAVE);
        chatMessage.setTimestamp(LocalDateTime.now());
        
        // DB에 퇴장 메시지 저장
        Message messageEntity = new Message(
            room.getId(),
            leaveMessage.getUserId().longValue(),
            Message.MessageType.CHAT,
            chatMessage.getContent()
        );
        messageRepository.save(messageEntity);
        
        // 통합 메시지로 퇴장 알림 전송
        com.vims.chat.dto.UnifiedMessage leaveMsg = 
            com.vims.chat.dto.UnifiedMessage.userLeave(chatMessage);
        messagingTemplate.convertAndSend("/room/" + leaveMessage.getRoomCode(), leaveMsg);
        
        log.info("User {} left room {}", leaveMessage.getUserName(), leaveMessage.getRoomCode());
    }

    @Transactional
    public void sendMessage(RoomChatMessage chatMessage) {
        // 사용자가 해당 방에 참여하고 있는지 확인
        if (!isUserInRoom(chatMessage.getRoomCode(), chatMessage.getSenderId())) {
            log.warn("User {} tried to send message to room {} but not a participant", 
                    chatMessage.getSenderId(), chatMessage.getRoomCode());
            return;
        }
        
        // 방이 존재하는지 확인
        Room room = roomService.findByRoomCode(chatMessage.getRoomCode())
                .orElse(null);
        
        if (room == null) {
            log.warn("Room not found with code: {}", chatMessage.getRoomCode());
            return;
        }
        
        chatMessage.setTimestamp(LocalDateTime.now());
        chatMessage.setType(RoomChatMessage.MessageType.CHAT);
        
        // DB에 채팅 메시지 저장
        Message messageEntity = new Message(
            room.getId(),
            chatMessage.getSenderId().longValue(),
            Message.MessageType.CHAT,
            chatMessage.getContent()
        );
        messageRepository.save(messageEntity);
        
        // 통합 메시지로 실시간 채팅 전송
        com.vims.chat.dto.UnifiedMessage realtimeMsg = 
            com.vims.chat.dto.UnifiedMessage.realtimeMessage(chatMessage);
        messagingTemplate.convertAndSend("/room/" + chatMessage.getRoomCode(), realtimeMsg);
        
        log.info("Room message sent from {} in room {}", 
                chatMessage.getSenderName(), chatMessage.getRoomCode());
    }

    // 방 채팅 히스토리 조회 (페이징)
    public List<Message> getRoomChatHistory(String roomCode, int page, int size) {
        try {
            Room room = roomService.findByRoomCode(roomCode).orElse(null);
            if (room == null) {
                log.warn("Room not found with code: {}", roomCode);
                return List.of();
            }
            
            Pageable pageable = PageRequest.of(page, size);
            
            log.info("Searching for room history: roomCode={}, roomId={}, page={}, size={}", 
                    roomCode, room.getId(), page, size);
            
            Page<Message> messagePage = messageRepository.findLectureChatMessages(room.getId(), pageable);
            
            log.info("Found {} messages for room {}", messagePage.getContent().size(), roomCode);
            
            return messagePage.getContent();
        } catch (Exception e) {
            log.error("Error retrieving room chat history for roomCode: {}", roomCode, e);
            return List.of();
        }
    }

    // 방 메시지 개수 조회
    public Long getRoomChatMessageCount(String roomCode) {
        Room room = roomService.findByRoomCode(roomCode).orElse(null);
        if (room == null) {
            return 0L;
        }
        return messageRepository.countLectureChatMessages(room.getId());
    }
    
    // 헬퍼 메서드들
    private boolean isUserInRoom(String roomCode, Integer userId) {
        Map<Integer, String> participants = roomParticipants.get(roomCode);
        return participants != null && participants.containsKey(userId);
    }
    
    private int getParticipantCount(String roomCode) {
        Map<Integer, String> participants = roomParticipants.get(roomCode);
        return participants != null ? participants.size() : 0;
    }
    
    public Map<Integer, String> getRoomParticipants(String roomCode) {
        return roomParticipants.getOrDefault(roomCode, Map.of());
    }
}