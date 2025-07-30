package com.vims.webrtc.service;

import com.vims.webrtc.domain.UserSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSessionService {
    private final ConcurrentHashMap<String, UserSession> userSessions = new ConcurrentHashMap<>();

    // ============ CRUD ============
    //사용자 세션 추가
    public void addUserSession(String sessionId, UserSession userSession) {
        userSessions.put(sessionId, userSession);
    }

    //세션 아이디로 사용자 세션 조회
    public UserSession getUserSession(String sessionId) {
       return userSessions.get(sessionId);
    }

    //전체 세션 조회
    public Collection<UserSession> getAllSessions() {
        return userSessions.values();
    }

    //사용자 세션 수정 없음
//    public void updateUserSession(String SessionId, UserSession userSession) {
//        userSessions.put(SessionId, userSession);
//    }

    //사용자 세션 삭제
    public UserSession removeUserSession(String sessionId){
        return userSessions.remove(sessionId);
    }

    // ============ 세션 상태 관리 메서드들 ============

    //연결이 끊긴 세션 삭제
    public void removeDisconnect(){
        userSessions.entrySet().removeIf(entry -> {
            WebSocketSession wsSession = entry.getValue().getWebSocketSession();
            boolean shouldRemove = wsSession == null || !wsSession.isOpen();
            if(shouldRemove){
                removeUserSession(entry.getValue().getSessionId());
            }
            return shouldRemove;
        });
    }

    // ============ 방별 집계 메서드들 ============

    //룸코드 기준, 같은 방 그룹핑
    public List<UserSession> getSessionsByRoom(String roomCode){
        return userSessions.values().stream()
                .filter(session -> session.getRoomCode().equals(roomCode))
                .collect(Collectors.toList());
    }
    //전체 세션 방별 그룹핑
    public Map<String, List<UserSession>> getSessionsGroupByRoom(){
        return userSessions.values().stream()
                .filter(session -> session.getRoomCode() != null)
                .collect(Collectors.groupingBy(UserSession::getRoomCode));
    }

    //방별 인원 조회
    public int getParticipantCount(String roomCode){
        return (int) userSessions.values().stream()
                .filter(sessions -> sessions.getRoomCode().equals(roomCode))
                .count();
    }
}
