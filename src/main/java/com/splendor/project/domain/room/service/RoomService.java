package com.splendor.project.domain.room.service;

import com.splendor.project.domain.player.dto.PlayerDto;
import com.splendor.project.domain.player.entity.Player;
import com.splendor.project.domain.room.dto.request.RequestRoomDto;
import com.splendor.project.domain.room.dto.response.ResponseRoomsDto;
import com.splendor.project.domain.room.entity.Room;
import com.splendor.project.domain.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class RoomService {

    private final RoomRepository roomRepository ;

    public List<ResponseRoomsDto> findAllRooms(){
        List<Room> rooms = roomRepository.findAll();
        return rooms.stream()
                .map(room -> {
                    List<Player> players = room.getPlayers();
                    String hostName = players.stream()
                            .filter(Player::isHosted)
                            .findFirst()
                            .map(Player::getNickname)
                            .orElse("방장 없음");

                    List<PlayerDto> playerDtos = players.stream().map(player -> new PlayerDto(player.getNickname(), player.isReady())).toList();
                    return new ResponseRoomsDto(room.getRoomName() , room.getRoomId() ,room.getRoomStatus() ,hostName , players.size() , playerDtos);
                }).toList();
    }

    public Optional<Room> findRoom(Long roomId){
        return roomRepository.findById(roomId);
    }



}
