package com.vims.room.repository;

import com.vims.room.entity.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<RoomEntity, Long> {
    
    Optional<RoomEntity> findByRoomCode(String roomCode);
    
    boolean existsByRoomCode(String roomCode);
    
    List<RoomEntity> findByHostUserId(Long hostUserId);
    
    @Query("SELECT r FROM Room r WHERE r.isOpenToEveryone = true ORDER BY r.createdAt DESC")
    List<RoomEntity> findOpenRooms();
    
    @Query("SELECT r FROM Room r WHERE r.title LIKE %:keyword% OR r.description LIKE %:keyword%")
    List<RoomEntity> findByTitleOrDescriptionContaining(@Param("keyword") String keyword);
}