package com.vims.webrtc.domain;

import org.kurento.client.Composite;
import org.kurento.client.HubPort;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class UserSession {
    private final String sessionId;
    private final String userName;
    private final WebSocketSession webSocketSession;
    //MCU를 위한 hub port, 조인하려는 room의 이미 존재하는 composite를 get해서 허브 포트 생성 및 연결
    private final HubPort hubPort;
    private String roomCode;
    private Long userId; // JWT 인증으로 얻은 userId 추가
    private String Mode;

    // 송신용 (클라이언트 → 서버)
    private WebRtcEndpoint outgoingMedia;

    // 수신용 (서버 → 클라이언트, 다른 참가자별)
    private final ConcurrentMap<String, WebRtcEndpoint> incomingMedia = new ConcurrentHashMap<>();

    public UserSession(String sessionId, String userName, WebSocketSession session, Composite composite, String mode) {
        this.sessionId = sessionId;
        this.userName = userName;
        this.webSocketSession = session;
        this.hubPort = new HubPort.Builder(composite).build();
        this.Mode = mode;
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
    public HubPort getHubPort() {return hubPort;}
    public String getMode() { return Mode; }
    public void setMode(String mode) { this.Mode = mode; }
}