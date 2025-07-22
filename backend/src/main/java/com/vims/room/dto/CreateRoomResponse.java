package com.vims.room.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoomResponse {
    private Long roomId;
    private String roomCode;
    private String title;
    private String description;
    private Integer hostUserId;
    private Integer maxParticipants;
    private Boolean isRecordingEnabled;
    private LocalDateTime createdAt;
    private Boolean autoJoin; // 방 생성 후 자동 입장 여부
    
    public CreateRoomResponse(Long roomId, String roomCode, String title, String description,
                            Integer hostUserId, Integer maxParticipants, Boolean isRecordingEnabled,
                            LocalDateTime createdAt) {
        this.roomId = roomId;
        this.roomCode = roomCode;
        this.title = title;
        this.description = description;
        this.hostUserId = hostUserId;
        this.maxParticipants = maxParticipants;
        this.isRecordingEnabled = isRecordingEnabled;
        this.createdAt = createdAt;
        this.autoJoin = true; // 기본적으로 자동 입장
    }
}