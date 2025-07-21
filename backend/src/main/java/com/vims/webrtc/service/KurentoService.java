package com.vims.webrtc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// import org.kurento.client.*;
// import org.kurento.jsonrpc.JsonUtils;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KurentoService {

    // private final KurentoClient kurentoClient;
    // private final Map<String, MediaPipeline> pipelines = new ConcurrentHashMap<>();
    // private final Map<String, WebRtcEndpoint> webRtcEndpoints = new ConcurrentHashMap<>();

    // Kurento 기능은 나중에 구현 - 지금은 채팅만 테스트
    /*
    public String processOffer(String sessionId, String sdpOffer) {
        log.info("Processing offer for session: {}", sessionId);
        
        try {
            // Create Media Pipeline
            MediaPipeline pipeline = kurentoClient.createMediaPipeline();
            pipelines.put(sessionId, pipeline);

            // Create WebRtcEndpoint
            WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
            webRtcEndpoints.put(sessionId, webRtcEndpoint);

            // Set ICE candidate handler
            webRtcEndpoint.addIceCandidateFoundListener(event -> {
                log.info("ICE candidate found for session {}: {}", sessionId, 
                    JsonUtils.toJsonObject(event.getCandidate()));
            });

            // Process SDP offer and generate answer
            String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);
            webRtcEndpoint.gatherCandidates();

            log.info("SDP answer generated for session: {}", sessionId);
            return sdpAnswer;

        } catch (Exception e) {
            log.error("Error processing offer for session {}: {}", sessionId, e.getMessage());
            throw new RuntimeException("Failed to process offer", e);
        }
    }

    public void addIceCandidate(String sessionId, IceCandidate candidate) {
        WebRtcEndpoint webRtcEndpoint = webRtcEndpoints.get(sessionId);
        if (webRtcEndpoint != null) {
            webRtcEndpoint.addIceCandidate(candidate);
            log.info("ICE candidate added for session: {}", sessionId);
        }
    }

    public void releaseSession(String sessionId) {
        log.info("Releasing session: {}", sessionId);
        
        WebRtcEndpoint webRtcEndpoint = webRtcEndpoints.remove(sessionId);
        if (webRtcEndpoint != null) {
            webRtcEndpoint.release();
        }

        MediaPipeline pipeline = pipelines.remove(sessionId);
        if (pipeline != null) {
            pipeline.release();
        }
    }

    public void connectUsers(String fromSessionId, String toSessionId) {
        WebRtcEndpoint fromEndpoint = webRtcEndpoints.get(fromSessionId);
        WebRtcEndpoint toEndpoint = webRtcEndpoints.get(toSessionId);
        
        if (fromEndpoint != null && toEndpoint != null) {
            fromEndpoint.connect(toEndpoint);
            toEndpoint.connect(fromEndpoint);
            log.info("Connected users: {} <-> {}", fromSessionId, toSessionId);
        }
    }
    */
}