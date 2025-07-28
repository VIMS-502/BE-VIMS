package com.vims.webrtc.domain;

import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.web.socket.TextMessage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Room {
    private final String name;
    private final MediaPipeline pipeline;
    private final ConcurrentMap<String, UserSession> participants = new ConcurrentHashMap<>();

    public Room(String name, MediaPipeline pipeline) {
        this.name = name;
        this.pipeline = pipeline;
    }

    public void join(UserSession newParticipant) throws Exception {
        participants.put(newParticipant.getUserName(), newParticipant);

        // 기존 참가자들에게 새 참가자 알림
        for (UserSession participant : participants.values()) {
            if (!participant.getUserName().equals(newParticipant.getUserName())) {
                participant.sendMessage(new TextMessage(
                        String.format("{\"type\":\"newParticipantArrived\",\"name\":\"%s\"}",
                                newParticipant.getUserName())
                ));
            }
        }

        // 새 참가자에게 기존 참가자 목록 전송
        for (UserSession participant : participants.values()) {
            if (!participant.getUserName().equals(newParticipant.getUserName())) {
                newParticipant.sendMessage(new TextMessage(
                        String.format("{\"type\":\"existingParticipant\",\"name\":\"%s\"}",
                                participant.getUserName())
                ));
            }
        }
    }

    public void leave(UserSession user) throws Exception {
        participants.remove(user.getUserName());

        // WebRTC endpoints 정리
        if (user.getOutgoingMedia() != null) {
            user.getOutgoingMedia().release();
        }

        for (WebRtcEndpoint endpoint : user.getIncomingMedia().values()) {
            endpoint.release();
        }

        // 다른 참가자들에게 알림
        for (UserSession participant : participants.values()) {
            participant.sendMessage(new TextMessage(
                    String.format("{\"type\":\"participantLeft\",\"name\":\"%s\"}",
                            user.getUserName())
            ));
        }
    }

    // Getters
    public String getName() { return name; }
    public MediaPipeline getPipeline() { return pipeline; }
    public ConcurrentMap<String, UserSession> getParticipants() { return participants; }
}
