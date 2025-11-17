package com.splendor.project.domain.room.controller;
import com.splendor.project.domain.player.service.PlayerService;
import com.splendor.project.domain.room.dto.request.RequestRoomDto;
import com.splendor.project.domain.room.dto.response.ResponseRoomDto;
import com.splendor.project.domain.room.dto.response.ResponseRoomsDto;
import com.splendor.project.domain.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.util.List;

import static com.splendor.project.contants.StompConstants.TOPIC_ROOM_SPECIFIC_PREFIX;

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
    public ResponseRoomDto addRoomsMessage(@Payload RequestRoomDto roomRequestDTO) {
         return  playerService.save(roomRequestDTO);
    }

    @MessageMapping("/join/room/{roomId}")
    public void joinRoomsMessage(@Payload RequestRoomDto requestRoomDto , @DestinationVariable Long roomId) {
        ResponseRoomDto responseRoomDto = playerService.join(requestRoomDto , roomId);

        messagingTemplate.convertAndSend("topic/rooms", responseRoomDto);

        String specificRoomTopic = TOPIC_ROOM_SPECIFIC_PREFIX + roomId;
        messagingTemplate.convertAndSend(specificRoomTopic , responseRoomDto);

    }

    @MessageMapping("/ready/room/{roomId}/{playerId}")
    public void readyRoomsMessage(@DestinationVariable Long roomId  ,@DestinationVariable String playerId) {

        ResponseRoomDto responseRoomDto = playerService.toggleReady(playerId, roomId);

        String specificRoomTopic = TOPIC_ROOM_SPECIFIC_PREFIX + roomId;
        messagingTemplate.convertAndSend(specificRoomTopic , responseRoomDto);

    }
}
