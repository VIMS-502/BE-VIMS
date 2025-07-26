package com.vims.room.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    private String description;
    
    @Column(name = "room_code", nullable = false, unique = true, length = 20)
    private String roomCode;
    
    @Column(name = "host_user_id", nullable = false)
    private Long hostUserId;
    
    @Column(name = "course_id")
    private Long courseId;
    
    @Column(name = "max_participants")
    private Integer maxParticipants = 10;
    
    private String password;
    
    @Column(name = "is_recording_enabled")
    private Boolean isRecordingEnabled = false;
    
    @Column(name = "scheduled_start_time")
    private LocalDateTime scheduledStartTime;
    
    @Column(name = "scheduled_end_time")
    private LocalDateTime scheduledEndTime;
    
    @Column(name = "is_open_to_everyone")
    private Boolean isOpenToEveryone = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}