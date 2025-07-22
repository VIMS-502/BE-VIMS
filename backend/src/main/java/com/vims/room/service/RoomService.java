package com.vims.room.service;

import com.vims.room.dto.CreateRoomRequest;
import com.vims.room.dto.CreateRoomResponse;
import com.vims.room.entity.Room;
import com.vims.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;

    @Transactional
    public CreateRoomResponse createRoom(CreateRoomRequest request) {
        // 고유한 room_code 생성
        String roomCode = generateUniqueRoomCode();
        
        Room room = new Room();
        room.setTitle(request.getTitle());
        room.setDescription(request.getDescription());
        room.setRoomCode(roomCode);
        room.setHostUserId(request.getHostUserId().longValue());
        room.setMaxParticipants(request.getMaxParticipants());
        room.setPassword(request.getPassword());
        room.setIsRecordingEnabled(request.getIsRecordingEnabled());
        room.setScheduledStartTime(request.getScheduledStartTime());
        room.setScheduledEndTime(request.getScheduledEndTime());
        room.setIsOpenToEveryone(request.getIsOpenToEveryone());
        room.setCreatedAt(LocalDateTime.now());
        
        Room savedRoom = roomRepository.save(room);
        
        log.info("Room created: {} with code: {} by user: {}", savedRoom.getTitle(), roomCode, savedRoom.getHostUserId());
        
        return new CreateRoomResponse(
            savedRoom.getId(),
            savedRoom.getRoomCode(),
            savedRoom.getTitle(),
            savedRoom.getDescription(),
            savedRoom.getHostUserId().intValue(),
            savedRoom.getMaxParticipants(),
            savedRoom.getIsRecordingEnabled(),
            savedRoom.getCreatedAt()
        );
    }
    
    public Optional<Room> findByRoomCode(String roomCode) {
        return roomRepository.findByRoomCode(roomCode);
    }
    
    public List<Room> getOpenRooms() {
        return roomRepository.findOpenRooms();
    }
    
    public List<Room> getUserRooms(Long hostUserId) {
        return roomRepository.findByHostUserId(hostUserId);
    }
    
    public List<Room> searchRooms(String keyword) {
        return roomRepository.findByTitleOrDescriptionContaining(keyword);
    }
    
    private String generateUniqueRoomCode() {
        String roomCode;
        do {
            roomCode = generateRoomCode();
        } while (roomRepository.existsByRoomCode(roomCode));
        return roomCode;
    }
    
    private String generateRoomCode() {
        // 6자리 대문자 + 숫자 조합으로 room code 생성
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return sb.toString();
    }
}