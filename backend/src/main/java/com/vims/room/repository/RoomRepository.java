package com.vims.room.repository;

import com.vims.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    
    Optional<Room> findByRoomCode(String roomCode);
    
    boolean existsByRoomCode(String roomCode);
    
    List<Room> findByHostUserId(Long hostUserId);
    
    @Query("SELECT r FROM Room r WHERE r.isOpenToEveryone = true ORDER BY r.createdAt DESC")
    List<Room> findOpenRooms();
    
    @Query("SELECT r FROM Room r WHERE r.title LIKE %:keyword% OR r.description LIKE %:keyword%")
    List<Room> findByTitleOrDescriptionContaining(@Param("keyword") String keyword);
}