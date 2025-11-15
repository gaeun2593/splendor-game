package com.splendor.project.domain.room.service;

import com.splendor.project.domain.room.dto.request.RequestRoomDto;
import com.splendor.project.domain.room.dto.response.ResponseRoomDto;
import com.splendor.project.domain.room.entity.Room;
import com.splendor.project.domain.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository ;

    public List<Room> findAllRooms(){
        return roomRepository.findAll();
    }


}
