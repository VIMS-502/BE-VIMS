package com.vims.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LectureJoinMessage {
    private String lectureId;
    private String userId;
    private String userName;
    private String userRole; // INSTRUCTOR, STUDENT, TA
}