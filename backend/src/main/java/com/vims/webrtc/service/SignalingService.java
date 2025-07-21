package com.vims.webrtc.service;

import com.vims.webrtc.dto.SignalingMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SignalingService {

    private final SimpMessagingTemplate messagingTemplate;
    
    // 방별 사용자 관리
    private final Map<String, Set<String>> roomUsers = new ConcurrentHashMap<>();
    // 사용자별 세션 ID 관리
    private final Map<String, String> userSessions = new ConcurrentHashMap<>();

    public void joinRoom(SignalingMessage message, String sessionId) {
        String roomId = message.getRoomId();
        String userId = message.getFrom();
        
        // 사용자를 방에 추가
        roomUsers.computeIfAbsent(roomId, k -> new CopyOnWriteArraySet<>()).add(userId);
        userSessions.put(userId, sessionId);
        
        // 방의 다른 사용자들에게 새 사용자 입장 알림
        SignalingMessage userJoinedMessage = new SignalingMessage(
            SignalingMessage.Type.USER_JOINED.name(),
            "server",
            null,
            roomId,
            userId
        );
        
        // 방의 모든 사용자에게 브로드캐스트 (자신 제외)
        roomUsers.get(roomId).stream()
            .filter(user -> !user.equals(userId))
            .forEach(user -> messagingTemplate.convertAndSendToUser(
                user, "/queue/webrtc", userJoinedMessage));
        
        // 새로 입장한 사용자에게 현재 방 사용자 목록 전송
        SignalingMessage roomUsersMessage = new SignalingMessage(
            SignalingMessage.Type.ROOM_USERS.name(),
            "server",
            userId,
            roomId,
            roomUsers.get(roomId)
        );
        
        messagingTemplate.convertAndSendToUser(userId, "/queue/webrtc", roomUsersMessage);
    }

    public void leaveRoom(SignalingMessage message, String sessionId) {
        String roomId = message.getRoomId();
        String userId = message.getFrom();
        
        // 사용자를 방에서 제거
        Set<String> users = roomUsers.get(roomId);
        if (users != null) {
            users.remove(userId);
            if (users.isEmpty()) {
                roomUsers.remove(roomId);
            }
        }
        userSessions.remove(userId);
        
        // 방의 다른 사용자들에게 사용자 퇴장 알림
        SignalingMessage userLeftMessage = new SignalingMessage(
            SignalingMessage.Type.USER_LEFT.name(),
            "server",
            null,
            roomId,
            userId
        );
        
        if (users != null) {
            users.forEach(user -> messagingTemplate.convertAndSendToUser(
                user, "/queue/webrtc", userLeftMessage));
        }
    }

    public void sendOffer(SignalingMessage message) {
        messagingTemplate.convertAndSendToUser(
            message.getTo(), "/queue/webrtc", message);
    }

    public void sendAnswer(SignalingMessage message) {
        messagingTemplate.convertAndSendToUser(
            message.getTo(), "/queue/webrtc", message);
    }

    public void sendIceCandidate(SignalingMessage message) {
        messagingTemplate.convertAndSendToUser(
            message.getTo(), "/queue/webrtc", message);
    }
}