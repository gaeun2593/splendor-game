package com.splendor.project.domain.room.repository;

import com.splendor.project.domain.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    @Override
    List<Room> findAll();

    @Override
    <S extends Room> S save(S entity);

    @Override
    Optional<Room> findById(Long roomId);
}
