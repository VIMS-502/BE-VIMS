package com.vims.webrtc.service;

import com.vims.webrtc.domain.Room;
import com.vims.webrtc.domain.UserSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomSessionService {
    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
    // ============ CRUD 메서드 ============

    //방 추가
    public void addRoom(String roomCode, Room room) {
        rooms.put(roomCode, room);
    }

    //방 조회
    public Room getRoom(String roomCode){
        return rooms.get(roomCode);
    }

    //방 전체 조회
    public Collection<Room> getRooms() {
        return rooms.values();
    }

    //방 업데이트
    public void updateRoom(String roomCode, Room room) {
        rooms.put(roomCode, room);
    }

    //방 삭제
    public Room removeRoom(String roomCode){
        return rooms.remove(roomCode);
    }

    // ============ 세션 관리 메서드 ============

    //모든 사용자와의 연결이 끊긴 방 삭제
    public boolean removeEmptyRoom(){
        return rooms.entrySet().removeIf( entry -> {
            String roomCode = entry.getKey();
            Room room = entry.getValue();

            boolean isDisconnect = room.getParticipants().isEmpty();
            if (isDisconnect) {
                log.info("Remove disconnect room = {}", roomCode);
                return true;
            }
            return false;
        });
    }

    // ============ participants 메서드 ============
}