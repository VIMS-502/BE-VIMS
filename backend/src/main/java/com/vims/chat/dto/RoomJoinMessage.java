package com.vims.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomJoinMessage {
    private String roomCode;
    private Long userId;
    private String userName;
    private String userRole; // INSTRUCTOR, STUDENT, TA
}