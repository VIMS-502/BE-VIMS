package com.vims.webrtc.handler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.vims.webrtc.domail.Room;
import com.vims.webrtc.domail.UserSession;

import org.kurento.client.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class SignalingHandler extends TextWebSocketHandler {

    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UserSession> sessions = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    @Autowired
    private KurentoClient kurento;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("새 연결: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);
            String messageType = jsonMessage.get("type").getAsString();

            System.out.println("받은 메시지: " + messageType + " from " + session.getId());

            switch (messageType) {
                case "joinRoom":
                    joinRoom(session, jsonMessage);
                    break;
                case "publishVideo":
                    publishVideo(session, jsonMessage);
                    break;
                case "receiveVideoFrom":
                    receiveVideoFrom(session, jsonMessage);
                    break;
                case "onIceCandidate":
                    onIceCandidate(session, jsonMessage);
                    break;
                case "leaveRoom":
                    leaveRoom(session);
                    break;
                default:
                    relayMessage(session, message);
            }
        } catch (Exception e) {
            System.err.println("메시지 처리 중 오류: " + e.getMessage());
            e.printStackTrace();

            // JSON 안전하게 생성
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("type", "error");
            errorResponse.addProperty("message", e.getMessage());
            session.sendMessage(new TextMessage(gson.toJson(errorResponse)));
        }
    }

    private void joinRoom(WebSocketSession session, JsonObject jsonMessage) throws Exception {
        if (kurento == null) {
            System.err.println("KurentoClient가 null입니다.");

            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("type", "error");
            errorResponse.addProperty("message", "Kurento Media Server에 연결할 수 없습니다.");
            session.sendMessage(new TextMessage(gson.toJson(errorResponse)));
            return;
        }

        // 세션에서 인증된 사용자 정보 가져오기
        Long userId = (Long) session.getAttributes().get("userId");
        String userName = (String) session.getAttributes().get("userName");
        
        if (userId == null || userName == null) {
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("type", "error");
            errorResponse.addProperty("message", "인증되지 않은 사용자입니다.");
            session.sendMessage(new TextMessage(gson.toJson(errorResponse)));
            return;
        }

        String roomName = jsonMessage.get("room").getAsString();

        System.out.println("=== 방 참가 디버그 ===");
        System.out.println("세션 ID: " + session.getId());
        System.out.println("인증된 사용자 ID: " + userId);
        System.out.println("인증된 사용자 이름: " + userName);
        System.out.println("방 이름: " + roomName);

        UserSession user = new UserSession(session.getId(), userName, session);
        user.setUserId(userId); // userId 추가 설정
        sessions.put(session.getId(), user);

        Room room = rooms.get(roomName);
        if (room == null) {
            try {
                MediaPipeline pipeline = kurento.createMediaPipeline();
                room = new Room(roomName, pipeline);
                rooms.put(roomName, room);
                System.out.println("새 방 생성: " + roomName);
            } catch (Exception e) {
                System.err.println("MediaPipeline 생성 실패: " + e.getMessage());
                sessions.remove(session.getId());
                return;
            }
        }

        user.setRoomName(roomName);
        room.join(user);

        System.out.println("현재 방의 참가자 수: " + room.getParticipants().size());
        System.out.println("==================");

        // 기존 참가자들에게 새 참가자 알림
        for (UserSession participant : room.getParticipants().values()) {
            if (!participant.getUserName().equals(userName)) {
                JsonObject newParticipantMessage = new JsonObject();
                newParticipantMessage.addProperty("type", "newParticipantArrived");
                newParticipantMessage.addProperty("name", userName);
                participant.sendMessage(new TextMessage(gson.toJson(newParticipantMessage)));
            }
        }

        // 새 참가자에게 기존 참가자들 알림
        for (UserSession participant : room.getParticipants().values()) {
            if (!participant.getUserName().equals(userName)) {
                JsonObject existingParticipantMessage = new JsonObject();
                existingParticipantMessage.addProperty("type", "existingParticipant");
                existingParticipantMessage.addProperty("name", participant.getUserName());
                session.sendMessage(new TextMessage(gson.toJson(existingParticipantMessage)));
            }
        }

        // 방 참가 완료 응답
        JsonObject joinedResponse = new JsonObject();
        joinedResponse.addProperty("type", "joinedRoom");
        joinedResponse.addProperty("room", roomName);
        session.sendMessage(new TextMessage(gson.toJson(joinedResponse)));

        System.out.println(userName + "이 " + roomName + " 방에 입장했습니다.");
    }

    private void publishVideo(WebSocketSession session, JsonObject jsonMessage) throws Exception {
        UserSession user = sessions.get(session.getId());
        if (user == null || user.getRoomName() == null) return;

        String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
        Room room = rooms.get(user.getRoomName());
        if (room == null) return;

        try {
            WebRtcEndpoint outgoingEndpoint = new WebRtcEndpoint.Builder(room.getPipeline()).build();
            user.setOutgoingMedia(outgoingEndpoint);

            outgoingEndpoint.addIceCandidateFoundListener(event -> {
                try {
                    if (user.getWebSocketSession().isOpen()) {
                        JsonObject iceCandidateMessage = new JsonObject();
                        iceCandidateMessage.addProperty("type", "iceCandidate");
                        iceCandidateMessage.addProperty("name", user.getUserName());
                        iceCandidateMessage.add("candidate", gson.toJsonTree(event.getCandidate()));

                        user.sendMessage(new TextMessage(gson.toJson(iceCandidateMessage)));
                    }
                } catch (Exception e) {
                    System.err.println("ICE 후보 전송 오류: " + e.getMessage());
                }
            });

            String sdpAnswer = outgoingEndpoint.processOffer(sdpOffer);
            outgoingEndpoint.gatherCandidates();

            // Gson을 사용하여 안전하게 JSON 생성
            JsonObject response = new JsonObject();
            response.addProperty("type", "publishVideoAnswer");
            response.addProperty("sdpAnswer", sdpAnswer);

            session.sendMessage(new TextMessage(gson.toJson(response)));

            // 비디오 게시 완료 후 다른 참가자들에게 즉시 알림
            notifyVideoAvailable(user, room);

            System.out.println(user.getUserName() + "의 비디오 송신이 시작되었습니다.");

            // 디버그 정보
            System.out.println("현재 방의 참가자 수: " + room.getParticipants().size());
            for (UserSession participant : room.getParticipants().values()) {
                System.out.println("참가자: " + participant.getUserName() +
                        ", 비디오 있음: " + (participant.getOutgoingMedia() != null));
            }

        } catch (Exception e) {
            System.err.println("비디오 게시 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 비디오 사용 가능 알림 즉시 전송
    private void notifyVideoAvailable(UserSession publisher, Room room) throws Exception {
        for (UserSession participant : room.getParticipants().values()) {
            if (!participant.getUserName().equals(publisher.getUserName())) {
                JsonObject videoAvailableMessage = new JsonObject();
                videoAvailableMessage.addProperty("type", "videoAvailable");
                videoAvailableMessage.addProperty("name", publisher.getUserName());

                participant.sendMessage(new TextMessage(gson.toJson(videoAvailableMessage)));
                System.out.println(publisher.getUserName() + "의 비디오 사용 가능 알림을 " +
                        participant.getUserName() + "에게 전송");
            }
        }
    }

    private void receiveVideoFrom(WebSocketSession session, JsonObject jsonMessage) throws Exception {
        UserSession user = sessions.get(session.getId());
        if (user == null || user.getRoomName() == null) {
            System.err.println("receiveVideoFrom: 사용자 세션 또는 방이 null");
            return;
        }

        String senderName = jsonMessage.get("sender").getAsString();
        String sdpOffer = jsonMessage.get("sdpOffer").getAsString();

        System.out.println("=== receiveVideoFrom 디버그 ===");
        System.out.println("수신자: " + user.getUserName());
        System.out.println("발신자: " + senderName);
        System.out.println("방: " + user.getRoomName());

        Room room = rooms.get(user.getRoomName());
        if (room == null) {
            System.err.println("방을 찾을 수 없음: " + user.getRoomName());
            return;
        }

        UserSession sender = room.getParticipants().get(senderName);
        if (sender == null) {
            System.err.println("발신자를 찾을 수 없음: " + senderName);
            System.out.println("현재 방 참가자:");
            for (String participantName : room.getParticipants().keySet()) {
                System.out.println("  - " + participantName);
            }

            JsonObject videoNotReadyMessage = new JsonObject();
            videoNotReadyMessage.addProperty("type", "videoNotReady");
            videoNotReadyMessage.addProperty("name", senderName);
            session.sendMessage(new TextMessage(gson.toJson(videoNotReadyMessage)));
            return;
        }

        if (sender.getOutgoingMedia() == null) {
            System.err.println("발신자의 미디어가 준비되지 않음: " + senderName);

            JsonObject videoNotReadyMessage = new JsonObject();
            videoNotReadyMessage.addProperty("type", "videoNotReady");
            videoNotReadyMessage.addProperty("name", senderName);
            session.sendMessage(new TextMessage(gson.toJson(videoNotReadyMessage)));
            return;
        }

        System.out.println("모든 조건 통과, WebRtcEndpoint 생성 시작");

        try {
            WebRtcEndpoint incomingEndpoint = new WebRtcEndpoint.Builder(room.getPipeline()).build();
            user.getIncomingMedia().put(senderName, incomingEndpoint);
            System.out.println("incomingEndpoint 생성 완료");

            incomingEndpoint.addIceCandidateFoundListener(event -> {
                try {
                    if (user.getWebSocketSession().isOpen()) {
                        JsonObject iceCandidateMessage = new JsonObject();
                        iceCandidateMessage.addProperty("type", "iceCandidate");
                        iceCandidateMessage.addProperty("name", senderName);
                        iceCandidateMessage.add("candidate", gson.toJsonTree(event.getCandidate()));

                        user.sendMessage(new TextMessage(gson.toJson(iceCandidateMessage)));
                    }
                } catch (Exception e) {
                    System.err.println("ICE 후보 전송 오류: " + e.getMessage());
                }
            });

            System.out.println("발신자 outgoing과 연결 시작");
            sender.getOutgoingMedia().connect(incomingEndpoint);
            System.out.println("연결 완료");

            System.out.println("SDP processOffer 시작");
            String sdpAnswer = incomingEndpoint.processOffer(sdpOffer);
            System.out.println("processOffer 완료");

            incomingEndpoint.gatherCandidates();
            System.out.println("gatherCandidates 완료");

            // ⭐ 핵심: Gson을 사용하여 안전하게 JSON 생성
            JsonObject response = new JsonObject();
            response.addProperty("type", "receiveVideoAnswer");
            response.addProperty("name", senderName);
            response.addProperty("sdpAnswer", sdpAnswer); // Gson이 자동으로 이스케이프 처리

            String responseJson = gson.toJson(response);

            System.out.println("receiveVideoAnswer 전송 완료 to " + user.getUserName() + " for " + senderName);

            session.sendMessage(new TextMessage(responseJson));

            System.out.println(user.getUserName() + "이 " + senderName + "의 비디오를 수신합니다.");
            System.out.println("=== receiveVideoFrom 완료 ===");

        } catch (Exception e) {
            System.err.println("비디오 수신 설정 오류: " + e.getMessage());
            e.printStackTrace();

            // 오류 발생시 클라이언트에게 알림
            JsonObject errorMessage = new JsonObject();
            errorMessage.addProperty("type", "error");
            errorMessage.addProperty("message", "비디오 수신 설정 오류: " + e.getMessage());
            session.sendMessage(new TextMessage(gson.toJson(errorMessage)));
        }
    }

    private void onIceCandidate(WebSocketSession session, JsonObject jsonMessage) {
        UserSession user = sessions.get(session.getId());
        if (user == null) return;

        String senderName = jsonMessage.get("name").getAsString();
        if (senderName == null) return;

        JsonObject candidateJson = jsonMessage.get("candidate").getAsJsonObject();
        if (candidateJson == null) return;

        try {
            IceCandidate candidate = new IceCandidate(
                    candidateJson.get("candidate").getAsString(),
                    candidateJson.get("sdpMid").getAsString(),
                    candidateJson.get("sdpMLineIndex").getAsInt()
            );

            if (user.getUserName().equals(senderName)) {
                if (user.getOutgoingMedia() != null) {
                    user.getOutgoingMedia().addIceCandidate(candidate);
                }
            } else {
                WebRtcEndpoint incomingEndpoint = user.getIncomingMedia().get(senderName);
                if (incomingEndpoint != null) {
                    incomingEndpoint.addIceCandidate(candidate);
                }
            }
        } catch (Exception e) {
            System.err.println("ICE 후보 처리 오류: " + e.getMessage());
        }
    }

    private void leaveRoom(WebSocketSession session) throws Exception {
        UserSession user = sessions.remove(session.getId());
        if (user != null && user.getRoomName() != null) {
            Room room = rooms.get(user.getRoomName());
            if (room != null) {
                try {
                    room.leave(user);

                    // 다른 참가자들에게 퇴장 알림
                    for (UserSession participant : room.getParticipants().values()) {
                        JsonObject participantLeftMessage = new JsonObject();
                        participantLeftMessage.addProperty("type", "participantLeft");
                        participantLeftMessage.addProperty("name", user.getUserName());
                        participant.sendMessage(new TextMessage(gson.toJson(participantLeftMessage)));
                    }

                    System.out.println(user.getUserName() + "이 " + user.getRoomName() + " 방을 떠났습니다.");

                    if (room.getParticipants().isEmpty()) {
                        rooms.remove(user.getRoomName());
                        room.getPipeline().release();
                        System.out.println("빈 방 제거: " + user.getRoomName());
                    }
                } catch (Exception e) {
                    System.err.println("방 나가기 처리 중 오류: " + e.getMessage());
                }
            }
        }
    }

    private void relayMessage(WebSocketSession session, TextMessage message) throws Exception {
        UserSession currentUser = sessions.get(session.getId());
        if (currentUser != null && currentUser.getRoomName() != null) {
            Room room = rooms.get(currentUser.getRoomName());
            if (room != null) {
                for (UserSession participant : room.getParticipants().values()) {
                    if (!participant.getSessionId().equals(session.getId()) &&
                            participant.getWebSocketSession().isOpen()) {
                        try {
                            participant.sendMessage(message);
                        } catch (Exception e) {
                            System.err.println("메시지 전달 오류: " + e.getMessage());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        try {
            leaveRoom(session);
            System.out.println("연결 종료: " + session.getId());
        } catch (Exception e) {
            System.err.println("연결 종료 처리 중 오류: " + e.getMessage());
        }
    }
}
