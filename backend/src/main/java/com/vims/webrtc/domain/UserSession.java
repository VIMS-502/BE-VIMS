package com.vims.webrtc.domain;

import org.kurento.client.WebRtcEndpoint;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class UserSession {
    private final String sessionId;
    private final String userName;
    private final WebSocketSession webSocketSession;
    private String roomCode;
    private Long userId; // JWT 인증으로 얻은 userId 추가
    
    // 송신용 (클라이언트 → 서버)
    private WebRtcEndpoint outgoingMedia;
    
    // 수신용 (서버 → 클라이언트, 다른 참가자별)
    private final ConcurrentMap<String, WebRtcEndpoint> incomingMedia = new ConcurrentHashMap<>();

    public UserSession(String sessionId, String userName, WebSocketSession session) {
        this.sessionId = sessionId;
        this.userName = userName;
        this.webSocketSession = session;
    }

    public void sendMessage(TextMessage message) throws Exception {
        synchronized (webSocketSession) {
            if (webSocketSession.isOpen()) {
                webSocketSession.sendMessage(message);
            }
        }
    }

    // Getters and Setters
    public String getSessionId() { return sessionId; }
    public String getUserName() { return userName; }
    public WebSocketSession getWebSocketSession() { return webSocketSession; }
    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public WebRtcEndpoint getOutgoingMedia() { return outgoingMedia; }
    public void setOutgoingMedia(WebRtcEndpoint outgoingMedia) { this.outgoingMedia = outgoingMedia; }
    public ConcurrentMap<String, WebRtcEndpoint> getIncomingMedia() { return incomingMedia; }
}
