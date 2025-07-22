package com.vims.lecture.controller;

import com.vims.lecture.service.LectureService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/lectures")
@RequiredArgsConstructor
public class LectureQueryController {

    private static final Logger log = LoggerFactory.getLogger(LectureQueryController.class);
    private final LectureService lectureService;

    @GetMapping("/{lectureId}/participants")
    public ResponseEntity<Map<String, Object>> getLectureParticipants(@PathVariable String lectureId) {
        
        Set<String> participants = lectureService.getLectureParticipants(lectureId);
        int participantCount = participants.size();
        boolean isActive = lectureService.isLectureActive(lectureId);
        
        return ResponseEntity.ok(Map.of(
            "lectureId", lectureId,
            "participants", participants,
            "participantCount", participantCount,
            "isActive", isActive
        ));
    }

    @GetMapping("/active")
    public ResponseEntity<Map<String, Integer>> getActiveLectures() {
        
        Map<String, Integer> activeLectures = lectureService.getActiveLectures();
        
        return ResponseEntity.ok(activeLectures);
    }

    @GetMapping("/{lectureId}/status")
    public ResponseEntity<Map<String, Object>> getLectureStatus(@PathVariable String lectureId) {
        
        boolean isActive = lectureService.isLectureActive(lectureId);
        int participantCount = lectureService.getLectureParticipantCount(lectureId);
        
        return ResponseEntity.ok(Map.of(
            "lectureId", lectureId,
            "isActive", isActive,
            "participantCount", participantCount
        ));
    }

    // 강의실 생성 API (테스트용)
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createLecture(@RequestBody Map<String, String> request) {
        String lectureId = request.getOrDefault("lectureId", "lecture_" + System.currentTimeMillis());
        String lectureName = request.getOrDefault("lectureName", "테스트 강의실");
        
        log.info("Creating lecture: {} ({})", lectureName, lectureId);
        
        return ResponseEntity.ok(Map.of(
            "lectureId", lectureId,
            "lectureName", lectureName,
            "status", "created",
            "message", "강의실이 생성되었습니다."
        ));
    }
}