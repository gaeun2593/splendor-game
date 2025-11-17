package com.splendor.project.domain.room.dto.response;

import com.splendor.project.domain.player.dto.PlayerDto;
import com.splendor.project.domain.room.entity.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ResponseRoomDto {
    private String roomName ;
    private Long roomId ;
    private RoomStatus roomStatus ;
    private String hostName ;
    private int playerCount ;
    List<PlayerDto> players  ;
    private String playerId;

}
