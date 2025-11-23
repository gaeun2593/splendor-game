package com.splendor.project.domain.player.service;

import com.splendor.project.domain.player.dto.PlayerDto;
import com.splendor.project.domain.player.entity.Player;
import com.splendor.project.domain.player.repository.PlayerRepository;
import com.splendor.project.domain.room.dto.request.RequestRoomDto;
import com.splendor.project.domain.room.dto.response.ResponseRoomDto;
import com.splendor.project.domain.room.entity.Room;
import com.splendor.project.domain.room.entity.RoomStatus;
import com.splendor.project.domain.room.repository.RoomRepository;
import com.splendor.project.exception.ErrorCode;
import com.splendor.project.exception.GameLogicException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final RoomRepository roomRepository ;

    @Transactional(readOnly = true)
    public List<Player> findAll(){
        return playerRepository.findAll();
    }

    public ResponseRoomDto save(RequestRoomDto requestRoomDto){
        String roomName = requestRoomDto.getRoomName();

        Room room = new Room(roomName , RoomStatus.WAITING);
        roomRepository.save(room);


        Player createdPlayer = new Player(room , requestRoomDto.getHostName() , true , false) ;
        Player savedPlayer = playerRepository.save(createdPlayer);


        return buildResponseRoomDto(room, savedPlayer);
    }

    public ResponseRoomDto join(RequestRoomDto requestRoomDto, Long roomId) {

        Room room = roomRepository.findById(roomId).orElseThrow(() -> new GameLogicException(ErrorCode.ROOM_NOT_FOUND));


        Player createdPlayer = new Player(room , requestRoomDto.getHostName() , true , false) ;
        Player savedPlayer = playerRepository.save(createdPlayer);


        return buildResponseRoomDto(room, savedPlayer);
    }

    public ResponseRoomDto toggleReady(String playerId , Long roomId) {

        // Room 찾기 실패 시 GameLogicException 사용
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new GameLogicException(ErrorCode.ROOM_NOT_FOUND));

        // Player 찾기 실패 시 GameLogicException 사용
        Player savedPlayer = playerRepository.findById(playerId)
                .orElseThrow(() -> new GameLogicException(ErrorCode.PLAYER_NOT_FOUND));

        savedPlayer.toggleReady();

        return buildResponseRoomDto(room, savedPlayer);
    }


    private List<PlayerDto> convertToPlayerDtos(List<Player> players) {
        return players.stream()
                .map(player -> new PlayerDto(player.getNickname(), player.isReady()))
                .toList();
    }

    private ResponseRoomDto buildResponseRoomDto(Room room, Player actionPlayer) {

        List<Player> players = room.getPlayers();

        // DTO 변환
        List<PlayerDto> playerDtos = convertToPlayerDtos(players);

        return new ResponseRoomDto(
                room.getRoomName(),
                room.getRoomId(),
                room.getRoomStatus(),
                actionPlayer.getNickname(),
                players.size(),
                playerDtos,
                actionPlayer.getPlayerId()
        );
    }
}