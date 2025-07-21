package com.vims.lecture.controller;

import com.vims.lecture.service.LectureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/lectures")
@RequiredArgsConstructor
@Slf4j
public class LectureQueryController {

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
}