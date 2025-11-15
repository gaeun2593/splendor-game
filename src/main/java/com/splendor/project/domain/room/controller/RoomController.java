package com.splendor.project.domain.room.controller;

import com.splendor.project.domain.player.entity.Player;
import com.splendor.project.domain.player.service.PlayerService;
import com.splendor.project.domain.room.dto.request.RequestRoomDto;
import com.splendor.project.domain.room.dto.response.ResponseRoomsDto;
import com.splendor.project.domain.room.entity.Room;
import com.splendor.project.domain.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService ;
    private final PlayerService playerService ;

    @MessageMapping("/rooms")
    @SendTo("/topic/rooms")
    public List<ResponseRoomsDto> sendRoomsMessage(){
        List<Room> allRooms = roomService.findAllRooms();

        List<ResponseRoomsDto> responseRoomsDto = allRooms.stream()
                .map(room -> {
                    List<Player> players = room.getPlayers();
                    String hostName = players.stream().filter(Player::isHosted).findFirst().map(Player::getNickname).orElse("방장 없음");
                    return new ResponseRoomsDto(room.getRoomName(), room.getRoomId() ,room.getRoomStatus() , hostName , players.size());
                }).toList();
        return responseRoomsDto;
    }

    @MessageMapping("/add/room")
    @SendTo("/topic/rooms")
    public ResponseRoomsDto addRoomsMessage(@Payload RequestRoomDto roomRequestDTO) {
        Player player = playerService.save(roomRequestDTO);
        Room room = player.getRoom();
        return new ResponseRoomsDto(room.getRoomName(), room.getRoomId(), room.getRoomStatus(), player.getNickname(), room.getPlayers().size());
    }

}
