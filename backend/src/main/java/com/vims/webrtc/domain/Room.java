package com.vims.webrtc.domain;

import org.kurento.client.Composite;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.web.socket.TextMessage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Room {
    private final String name;
    private final MediaPipeline pipeline;
    private final Composite composite;
    private final int threshold;
    private final ConcurrentMap<String, UserSession> participants = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UserSession> sfuParticipants = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UserSession> mcuParticipants = new ConcurrentHashMap<>();


    public Room(String name, MediaPipeline pipeline) {
        this.name = name;
        this.pipeline = pipeline;
        this.threshold = 6;
//MCU용 합성기
        this.composite = new Composite.Builder(pipeline).build();
    }

    public void join(UserSession newParticipant) throws Exception {
        participants.put(newParticipant.getSessionId(), newParticipant);
        // SFU 인원 임계치 미만
        if(newParticipant.getMode().equals("SFU")){
            sfuParticipants.put(newParticipant.getSessionId(), newParticipant);
        }
        //인원 임계치 초과
        else{
            mcuParticipants.put(newParticipant.getSessionId(), newParticipant);
        }
        // 기존 참가자들에게 새 참가자 알림
        for (UserSession participant : participants.values()) {
            if (!participant.getSessionId().equals(newParticipant.getSessionId())) {
                participant.sendMessage(new TextMessage(
                        String.format("{\"type\":\"newParticipantArrived\",\"name\":\"%s\"}",
                                newParticipant.getUserName())
                ));
            }
        }

// 새 참가자에게 기존 참가자 목록 전송
        for (UserSession participant : participants.values()) {
            if (!participant.getSessionId().equals(newParticipant.getSessionId())) {
                newParticipant.sendMessage(new TextMessage(
                        String.format("{\"type\":\"existingParticipant\",\"name\":\"%s\"}",
                                participant.getUserName())
                ));
            }
        }
    }

    public void leave(UserSession user) throws Exception {
        participants.remove(user.getSessionId());
        if(user.getMode().equals("SFU")){
            sfuParticipants.remove(user.getSessionId());
        }
        //인원 임계치 초과
        else{
            mcuParticipants.remove(user.getSessionId());
        }
// WebRTC endpoints 정리
        if (user.getOutgoingMedia() != null) {
            user.getOutgoingMedia().release();
        }

        for (WebRtcEndpoint endpoint : user.getIncomingMedia().values()) {
            endpoint.release();
        }

        //MCU hub port 해제
        if (user.getHubPort() != null) {
            user.getHubPort().release();
        }

// 다른 참가자들에게 알림
        for (UserSession participant : participants.values()) {
            participant.sendMessage(new TextMessage(
                    String.format("{\"type\":\"participantLeft\",\"name\":\"%s\"}",
                            user.getUserName())
            ));
        }
    }

    public int SFUCount() {
        return sfuParticipants.size();
    }

    public int MCUCount() {
        return mcuParticipants.size();
    }

    public int participantsCount() {
        return participants.size();
    }
    // Getters
    public String getName() { return name; }
    public MediaPipeline getPipeline() { return pipeline; }
    public ConcurrentMap<String, UserSession> getParticipants() { return participants; }
    public Composite getComposite() {return composite;}
    public ConcurrentMap<String, UserSession> getSfuParticipants() { return sfuParticipants; }
    public ConcurrentMap<String, UserSession> getMcuParticipants() { return mcuParticipants; }
    public int getThreshold() {
        return threshold;
    }
}