package com.vims.room.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoomRequest {
    private String title;
    private String description;
    private Integer hostUserId;
    private Integer maxParticipants = 10;
    private String password; // Optional
    private Boolean isRecordingEnabled = false;
    private LocalDateTime scheduledStartTime; // Optional
    private LocalDateTime scheduledEndTime; // Optional
    private Boolean isOpenToEveryone = false;
}