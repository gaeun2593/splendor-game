package com.splendor.project.domain.game.service;

import com.splendor.project.domain.game.dto.response.BoardStateDto;
import com.splendor.project.domain.game.dto.response.GamePlayerDto;
import com.splendor.project.domain.game.dto.response.GameStateDto;
import com.splendor.project.domain.game.dto.response.PlayerStateDto;
import com.splendor.project.domain.game.repository.GameStateRepository;
import com.splendor.project.domain.player.entity.Player;
import com.splendor.project.domain.room.entity.Room;
import com.splendor.project.domain.room.repository.RoomRepository;
import com.splendor.project.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.splendor.project.domain.data.GemType.*;

@Service
@RequiredArgsConstructor
@Transactional
public class PlayGameService {

    private final InitialGameService initialGameService;
    private final RoomRepository roomRepository;
    private final GameStateRepository gameStateRepository; // Redis Repository

    public GameStateDto gameStart(Long roomId) {
        // 1. 보드 및 방 정보 초기화
        BoardStateDto boardStateDto = initialGameService.initializeGame();
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NoSuchElementException(ErrorCode.ROOM_NOT_FOUND.getMessage()));

        // 2. 플레이어 순서 섞기 및 현재 턴 플레이어 설정
        List<Player> players = room.getPlayers();
        Collections.shuffle(players);

        Player startingPlayer = players.get(0);
        GamePlayerDto gamePlayerDto = new GamePlayerDto(startingPlayer.getNickname(), startingPlayer.getPlayerId());

        // 3. 플레이어 초기 상태 목록 생성
        List<PlayerStateDto> playerStateDtos = players.stream()
                .map(player -> new PlayerStateDto(
                        new GamePlayerDto(player.getNickname(), player.getPlayerId()),
                        0, // 초기 점수
                        Map.of(DIAMOND, 0, RUBY, 0, EMERALD, 0, SAPPHIRE, 0, ONYX, 0, GOLD, 0), // 초기 토큰
                        Map.of(DIAMOND, 0, RUBY, 0, EMERALD, 0, SAPPHIRE, 0, ONYX, 0, GOLD, 0)  // 초기 보너스
                ))
                .toList();

        // 4. GameStateDto 생성
        GameStateDto gameStateDto = new GameStateDto(
                boardStateDto,
                playerStateDtos,
                room.getRoomId(),
                gamePlayerDto
        );

        // 5. Redis에 게임 상태 저장
        gameStateRepository.save(gameStateDto);

        return gameStateDto;
    }
}