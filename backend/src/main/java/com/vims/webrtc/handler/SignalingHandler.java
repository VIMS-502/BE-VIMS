package com.vims.webrtc.handler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.vims.webrtc.domain.Room;
import com.vims.webrtc.domain.UserSession;
import com.vims.webrtc.service.RoomSessionService;
import com.vims.webrtc.service.UserSessionService;

import org.kurento.client.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

@Component
public class SignalingHandler extends TextWebSocketHandler {

    private final RoomSessionService roomSessionService;
    private final UserSessionService userSessionService;
    private final Gson gson = new Gson();

    @Autowired
    private KurentoClient kurento;

    public SignalingHandler(RoomSessionService roomSessionService, UserSessionService userSessionService) {
        this.roomSessionService = roomSessionService;
        this.userSessionService = userSessionService;
    }

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

        String roomCode = jsonMessage.get("room").getAsString();

        System.out.println("=== 방 참가 디버그 ===");
        System.out.println("세션 ID: " + session.getId());
        System.out.println("인증된 사용자 ID: " + userId);
        System.out.println("인증된 사용자 이름: " + userName);
        System.out.println("방 이름: " + roomCode);

        //composite를 get해와야 UserSession의 hubPort 생성 가능
        Room room = roomSessionService.getRoom(roomCode);

        if (room == null) {
            try {
                MediaPipeline pipeline = kurento.createMediaPipeline();
                room = new Room(roomCode, pipeline);
                roomSessionService.addRoom(roomCode, room);
                System.out.println("새 방 생성: " + roomCode);
            } catch (Exception e) {
                System.err.println("MediaPipeline 생성 실패: " + e.getMessage());
                userSessionService.removeUserSession(session.getId());
                return;
            }
        }

        String mode;
        //방 참가 인원이 임계치(6) 이상이라면?
        if(room.SFUCount() >= room.getThreshold()){
            System.out.println("SFU 인원이 꽉 찼습니다. 지금 입장하는 " + userName + "는 MCU입니다.");
            mode = "MCU";
        }else{
            mode = "SFU";
        }

        UserSession user = new UserSession(session.getId(), userName, session, room.getComposite(), mode);
        user.setUserId(userId); // userId 추가 설정
        user.setRoomCode(roomCode);
        user.setMode(mode);
        room.join(user);

        //사용자 정보 설정 완료 + 방에 추가됨 => UserSession 목록에 추가
        userSessionService.addUserSession(session.getId(), user);


        System.out.println("현재 방의 참가자 수: " + room.getParticipants().size());
        System.out.println("==================");

        // 방 참가 완료 응답
        JsonObject joinedResponse = new JsonObject();
        joinedResponse.addProperty("type", "joinedRoom");
        joinedResponse.addProperty("room", roomCode);
        joinedResponse.addProperty("userName", userName); // 실제 사용자 이름 추가
        joinedResponse.addProperty("userId", userId);
        joinedResponse.addProperty("userSessionId", session.getId());
        joinedResponse.addProperty("connectionMode", user.getMode()); // 접속 방식 추가
        session.sendMessage(new TextMessage(gson.toJson(joinedResponse)));

        System.out.println(userName + "이 " + roomCode + " 방에 입장했습니다.");
    }

    private void publishVideo(WebSocketSession session, JsonObject jsonMessage) throws Exception {
        UserSession user = (UserSession) userSessionService.getUserSession(session.getId());
        if(user == null || user.getRoomCode() == null) return;

        String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
        Room room = roomSessionService.getRoom(user.getRoomCode());

        if (room == null) {
            System.err.println("방을 찾을 수 없음: " + user.getRoomCode());
            return;
        }

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
            // WebRtcEndpoint와 HubPort 연결 (핵심 차이점!)
            if(user.getMode().equals("MCU")) {
                outgoingEndpoint.connect(user.getHubPort());
            }

            String sdpAnswer = outgoingEndpoint.processOffer(sdpOffer);
            outgoingEndpoint.gatherCandidates();

            // Gson을 사용하여 안전하게 JSON 생성
            JsonObject response = new JsonObject();
            response.addProperty("type", "publishVideoAnswer");
            response.addProperty("sdpAnswer", sdpAnswer);

            user.getWebSocketSession().sendMessage(new TextMessage(gson.toJson(response)));

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
            if (!participant.getSessionId().equals(publisher.getSessionId())) {
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
        UserSession user = userSessionService.getUserSession(session.getId());
        if (user == null || user.getRoomCode() == null) {
            System.err.println("receiveVideoFrom: 사용자 세션 또는 방이 null");
            return;
        }

        // 🔥 핵심: 모드에 따라 분기 처리
        if ("SFU".equals(user.getMode())) {
            handleSFUReceiveVideo(user, jsonMessage);
        } else {
            handleMCUReceiveVideo(user, jsonMessage);
        }

    }

    private void handleMCUReceiveVideo(UserSession user, JsonObject jsonMessage) throws Exception {
        String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
        Room room = roomSessionService.getRoom(user.getRoomCode());

        System.out.println("=== MCU receiveVideoFrom 디버그 ===");
        System.out.println("수신자: " + user.getUserName());
        System.out.println("방: " + user.getRoomCode());
        System.out.println("모드: MCU (Composite 스트림 수신)");

        try {
            System.out.println("MCU WebRtcEndpoint 생성 시작");
            // MCU: Composite에서 믹싱된 스트림을 받음
            WebRtcEndpoint incomingEndpoint = new WebRtcEndpoint.Builder(room.getPipeline()).build();
            user.getIncomingMedia().put("composite", incomingEndpoint);
            System.out.println("MCU incomingEndpoint 생성 완료");
            // Composite 출력용 HubPort 생성
            HubPort compositeOutputPort = new HubPort.Builder(room.getComposite()).build();

            // Composite → 수신자로 연결 (개별 발신자가 아님!)
            compositeOutputPort.connect(incomingEndpoint);

            // ICE 후보 리스너
            incomingEndpoint.addIceCandidateFoundListener(event -> {
                try {
                    if (user.getWebSocketSession().isOpen()) {
                        JsonObject iceCandidateMessage = new JsonObject();
                        iceCandidateMessage.addProperty("type", "iceCandidate");
                        iceCandidateMessage.addProperty("name", "composite");
                        iceCandidateMessage.add("candidate", gson.toJsonTree(event.getCandidate()));
                        user.sendMessage(new TextMessage(gson.toJson(iceCandidateMessage)));
                    }
                } catch (Exception e) {
                    System.err.println("MCU ICE 후보 전송 오류: " + e.getMessage());
                }
            });

            // SDP 처리
            String sdpAnswer = incomingEndpoint.processOffer(sdpOffer);
            incomingEndpoint.gatherCandidates();

            JsonObject response = new JsonObject();
            response.addProperty("type", "receiveVideoAnswer");
            response.addProperty("name", "composite");
            response.addProperty("sdpAnswer", sdpAnswer);

            user.getWebSocketSession().sendMessage(new TextMessage(gson.toJson(response)));

            System.out.println(user.getUserName() + "이 MCU 믹싱된 스트림을 수신합니다.");
            System.out.println("=== MCU receiveVideoFrom 완료 ===");

        } catch (Exception e) {
            System.err.println("MCU 비디오 수신 설정 오류: " + e.getMessage());
            JsonObject errorMessage = new JsonObject();
            errorMessage.addProperty("type", "error");
            errorMessage.addProperty("message", "MCU 비디오 수신 설정 오류: " + e.getMessage());
            user.getWebSocketSession().sendMessage(new TextMessage(gson.toJson(errorMessage)));
            e.printStackTrace();
        }
    }

    private void handleSFUReceiveVideo(UserSession user, JsonObject jsonMessage) throws Exception {

        String senderSessionId = jsonMessage.get("sender").getAsString();
        String senderName = jsonMessage.get("name").getAsString();
        String sdpOffer = jsonMessage.get("sdpOffer").getAsString();

        System.out.println("=== receiveVideoFrom 디버그 ===");
        System.out.println("수신자: " + user.getUserName());
        System.out.println("발신자: " + senderName);
        System.out.println("방: " + user.getRoomCode());

        Room room = roomSessionService.getRoom(user.getRoomCode());
        if (room == null) {
            System.err.println("방을 찾을 수 없음: " + user.getRoomCode());
            return;
        }

        UserSession sender = room.getParticipants().get(senderSessionId);
        if (sender == null) {
            System.err.println("발신자를 찾을 수 없음: " + senderName);
            System.out.println("현재 방 참가자:");
            for (String participantName : room.getParticipants().keySet()) {
                System.out.println("  - " + participantName);
            }

            JsonObject videoNotReadyMessage = new JsonObject();
            videoNotReadyMessage.addProperty("type", "videoNotReady");
            videoNotReadyMessage.addProperty("name", senderName);
            user.getWebSocketSession().sendMessage(new TextMessage(gson.toJson(videoNotReadyMessage)));
            return;
        }

        if (sender.getOutgoingMedia() == null) {
            System.err.println("발신자의 미디어가 준비되지 않음: " + senderName);

            JsonObject videoNotReadyMessage = new JsonObject();
            videoNotReadyMessage.addProperty("type", "videoNotReady");
            videoNotReadyMessage.addProperty("name", senderName);
            user.getWebSocketSession().sendMessage(new TextMessage(gson.toJson(videoNotReadyMessage)));
            return;
        }

        System.out.println("모든 조건 통과, WebRtcEndpoint 생성 시작");

        try {
            WebRtcEndpoint incomingEndpoint = new WebRtcEndpoint.Builder(room.getPipeline()).build();
            user.getIncomingMedia().put(senderSessionId, incomingEndpoint);
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

            //  Gson을 사용하여 안전하게 JSON 생성
            JsonObject response = new JsonObject();
            response.addProperty("type", "receiveVideoAnswer");
            response.addProperty("name", senderName);
            String responseJson = gson.toJson(response);

            System.out.println("receiveVideoAnswer 전송 완료 to " + user.getUserName() + " for " + senderName);

            user.getWebSocketSession().sendMessage(new TextMessage(responseJson));
            response.addProperty("sdpAnswer", sdpAnswer); // Gson이 자동으로 이스케이프 처리


            System.out.println(user.getUserName() + "이 " + senderName + "의 비디오를 수신합니다.");
            System.out.println("=== receiveVideoFrom 완료 ===");

        } catch (Exception e) {
            System.err.println("비디오 수신 설정 오류: " + e.getMessage());
            e.printStackTrace();

            // 오류 발생시 클라이언트에게 알림
            JsonObject errorMessage = new JsonObject();
            errorMessage.addProperty("type", "error");
            errorMessage.addProperty("message", "비디오 수신 설정 오류: " + e.getMessage());
            user.getWebSocketSession().sendMessage(new TextMessage(gson.toJson(errorMessage)));
        }
    }

    private void onIceCandidate(WebSocketSession session, JsonObject jsonMessage) {
        UserSession user = userSessionService.getUserSession(session.getId());
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
                // 송신용 ICE 후보 (SFU/MCU 동일)
                if (user.getOutgoingMedia() != null) {
                    user.getOutgoingMedia().addIceCandidate(candidate);
                }
            } else {
                //수신용 ICE 후보 - SFU/MCU 분기 처리
                if ("SFU".equals(user.getMode())) {
                    // SFU: 개별 발신자별 엔드포인트
                    WebRtcEndpoint incomingEndpoint = user.getIncomingMedia().get(senderName);
                    if (incomingEndpoint != null) {
                        incomingEndpoint.addIceCandidate(candidate);
                    }
                } else {
                    // MCU: composite 엔드포인트만 사용
                    WebRtcEndpoint incomingEndpoint = user.getIncomingMedia().get("composite");
                    if (incomingEndpoint != null) {
                        incomingEndpoint.addIceCandidate(candidate);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("ICE 후보 처리 오류: " + e.getMessage());
        }
    }

    private void leaveRoom(WebSocketSession session) throws Exception {
        UserSession user = userSessionService.getUserSession(session.getId());
        if (user != null && user.getRoomCode() != null) {
            Room room = roomSessionService.getRoom(user.getRoomCode());
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

                    System.out.println(user.getUserName() + "이 " + user.getRoomCode() + " 방을 떠났습니다.");

                    if (room.getParticipants().isEmpty()) {
                        roomSessionService.removeRoom(user.getRoomCode());
                        room.getPipeline().release();
                        System.out.println("빈 방 제거: " + user.getRoomCode());
                    }
                } catch (Exception e) {
                    System.err.println("방 나가기 처리 중 오류: " + e.getMessage());
                }
            }
        }
    }

    private void relayMessage(WebSocketSession session, TextMessage message) throws Exception {
        UserSession currentUser = userSessionService.getUserSession(session.getId());
        if (currentUser != null && currentUser.getRoomCode() != null) {
            Room room = roomSessionService.getRoom(currentUser.getRoomCode());
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
