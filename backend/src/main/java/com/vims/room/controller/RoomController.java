package com.vims.room.controller;

import com.vims.room.dto.CreateRoomRequest;
import com.vims.room.dto.CreateRoomResponse;
import com.vims.room.entity.Room;
import com.vims.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Slf4j
public class RoomController {

    private final RoomService roomService;

    @PostMapping("/create")
    public ResponseEntity<CreateRoomResponse> createRoom(@RequestBody CreateRoomRequest request) {
        log.info("Creating room: {}", request.getTitle());
        
        CreateRoomResponse response = roomService.createRoom(request);
        
        log.info("Room created successfully: {} with code: {}", response.getTitle(), response.getRoomCode());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{roomCode}")
    public ResponseEntity<Room> getRoomByCode(@PathVariable String roomCode) {
        log.info("Getting room info for code: {}", roomCode);
        
        Optional<Room> room = roomService.findByRoomCode(roomCode);
        
        if (room.isPresent()) {
            return ResponseEntity.ok(room.get());
        } else {
            log.warn("Room not found for code: {}", roomCode);
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/open")
    public ResponseEntity<List<Room>> getOpenRooms() {
        log.info("Getting all open rooms");
        
        List<Room> openRooms = roomService.getOpenRooms();
        
        return ResponseEntity.ok(openRooms);
    }
    
    @GetMapping("/user/{hostUserId}")
    public ResponseEntity<List<Room>> getUserRooms(@PathVariable Long hostUserId) {
        log.info("Getting rooms for user: {}", hostUserId);
        
        List<Room> userRooms = roomService.getUserRooms(hostUserId);
        
        return ResponseEntity.ok(userRooms);
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<Room>> searchRooms(@RequestParam String keyword) {
        log.info("Searching rooms with keyword: {}", keyword);
        
        List<Room> searchResults = roomService.searchRooms(keyword);
        
        return ResponseEntity.ok(searchResults);
    }
}