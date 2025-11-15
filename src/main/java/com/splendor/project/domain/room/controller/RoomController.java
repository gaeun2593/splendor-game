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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.util.List;


@Controller
@RequiredArgsConstructor
public class RoomController {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomService roomService ;
    private final PlayerService playerService ;

    @MessageMapping("/rooms")
    @SendTo("/topic/rooms")
    public List<ResponseRoomsDto> sendRoomsMessage(){
        List<ResponseRoomsDto> allRooms = roomService.findAllRooms();


        return allRooms;
    }

    @MessageMapping("/add/room")
    @SendTo("/topic/update/rooms")
    public ResponseRoomsDto addRoomsMessage(@Payload RequestRoomDto roomRequestDTO) {
        Player player = playerService.save(roomRequestDTO);
        Room room = player.getRoom();
        return new ResponseRoomsDto(room.getRoomName(), room.getRoomId(), room.getRoomStatus(), player.getNickname(), room.getPlayers().size());
    }


}
