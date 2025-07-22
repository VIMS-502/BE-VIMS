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
    
    // Explicit getters and setters (in case Lombok doesn't work)
    public String getLectureId() { return lectureId; }
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getUserRole() { return userRole; }
    
    public void setLectureId(String lectureId) { this.lectureId = lectureId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setUserRole(String userRole) { this.userRole = userRole; }
}