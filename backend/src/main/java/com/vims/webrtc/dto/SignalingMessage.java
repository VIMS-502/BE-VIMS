package com.vims.webrtc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignalingMessage {
    private String type;
    private String from;
    private String to;
    private String roomId;
    private Object data;
    
    public enum Type {
        JOIN_ROOM,
        LEAVE_ROOM,
        OFFER,
        ANSWER,
        ICE_CANDIDATE,
        USER_JOINED,
        USER_LEFT,
        ROOM_USERS
    }
}