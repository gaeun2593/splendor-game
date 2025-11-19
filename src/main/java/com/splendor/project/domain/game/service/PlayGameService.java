package com.splendor.project.domain.game.service;

import com.splendor.project.domain.game.dto.response.BoardStateDto;
import com.splendor.project.domain.game.dto.response.GamePlayerDto;
import com.splendor.project.domain.game.dto.response.GameStateDto;
import com.splendor.project.domain.game.dto.response.PlayerStateDto;
import com.splendor.project.domain.player.entity.Player;
import com.splendor.project.domain.room.entity.Room;
import com.splendor.project.domain.room.repository.RoomRepository;
import com.splendor.project.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.splendor.project.domain.data.GemType.*;
import static com.splendor.project.domain.data.GemType.GOLD;
import static com.splendor.project.domain.data.GemType.ONYX;
import static com.splendor.project.domain.data.GemType.RUBY;

@Service
@RequiredArgsConstructor
@Transactional
public class PlayGameService {

    private final InitialGameService initialGameService ;
    private final RoomRepository roomRepository ;


    public GameStateDto gameStart(Long roomId){
        BoardStateDto boardStateDto = initialGameService.initializeGame();
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new NoSuchElementException(ErrorCode.ROOM_NOT_FOUND.getMessage()));
        List<Player> players = room.getPlayers();
        // 무작위 턴
        Collections.shuffle(players);
        GamePlayerDto gamePlayerDto = new GamePlayerDto(players.get(0).getNickname(), players.get(0).getPlayerId());
        List<PlayerStateDto> playerStateDtos = players.stream().map(player -> new PlayerStateDto(new GamePlayerDto(player.getNickname(), player.getPlayerId()), 0
                , Map.of(DIAMOND, 0, RUBY, 0, EMERALD, 0, SAPPHIRE, 0, ONYX, 0, GOLD, 0), Map.of(DIAMOND, 0, RUBY, 0, EMERALD, 0, SAPPHIRE, 0, ONYX, 0, GOLD, 0))).toList();
       return new GameStateDto(boardStateDto , playerStateDtos, room.getRoomId() , gamePlayerDto);
    }
}
