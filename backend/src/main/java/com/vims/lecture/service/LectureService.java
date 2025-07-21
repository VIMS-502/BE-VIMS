package com.vims.lecture.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Set;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LectureService {

    // 강의실별 참여자 관리
    private final Map<String, Set<String>> lectureParticipants = new ConcurrentHashMap<>();
    
    // 사용자별 현재 참여 강의 관리
    private final Map<String, String> userCurrentLecture = new ConcurrentHashMap<>();

    public boolean joinLecture(String lectureId, String userId) {
        if (lectureId == null || userId == null) {
            return false;
        }

        // 기존 강의에서 나가기
        String currentLecture = userCurrentLecture.get(userId);
        if (currentLecture != null && !currentLecture.equals(lectureId)) {
            leaveLecture(currentLecture, userId);
        }

        // 새 강의 참여
        lectureParticipants.computeIfAbsent(lectureId, k -> new CopyOnWriteArraySet<>()).add(userId);
        userCurrentLecture.put(userId, lectureId);

        log.info("User {} joined lecture {}. Current participants: {}", 
                userId, lectureId, lectureParticipants.get(lectureId).size());
        
        return true;
    }

    public boolean leaveLecture(String lectureId, String userId) {
        if (lectureId == null || userId == null) {
            return false;
        }

        Set<String> participants = lectureParticipants.get(lectureId);
        if (participants != null) {
            participants.remove(userId);
            if (participants.isEmpty()) {
                lectureParticipants.remove(lectureId);
                log.info("Lecture {} closed - no participants remaining", lectureId);
            }
        }

        userCurrentLecture.remove(userId);

        log.info("User {} left lecture {}. Remaining participants: {}", 
                userId, lectureId, participants != null ? participants.size() : 0);
        
        return true;
    }

    public Set<String> getLectureParticipants(String lectureId) {
        return lectureParticipants.getOrDefault(lectureId, Set.of());
    }

    public String getUserCurrentLecture(String userId) {
        return userCurrentLecture.get(userId);
    }

    public int getLectureParticipantCount(String lectureId) {
        Set<String> participants = lectureParticipants.get(lectureId);
        return participants != null ? participants.size() : 0;
    }

    public boolean isUserInLecture(String lectureId, String userId) {
        Set<String> participants = lectureParticipants.get(lectureId);
        return participants != null && participants.contains(userId);
    }

    public boolean isLectureActive(String lectureId) {
        return lectureParticipants.containsKey(lectureId) && 
               !lectureParticipants.get(lectureId).isEmpty();
    }

    public Map<String, Integer> getActiveLectures() {
        Map<String, Integer> activeLectures = new ConcurrentHashMap<>();
        lectureParticipants.forEach((lectureId, participants) -> {
            if (!participants.isEmpty()) {
                activeLectures.put(lectureId, participants.size());
            }
        });
        return activeLectures;
    }

    // 세션 종료 시 정리
    public void cleanupUserSession(String userId) {
        String currentLecture = userCurrentLecture.get(userId);
        if (currentLecture != null) {
            leaveLecture(currentLecture, userId);
        }
    }
}