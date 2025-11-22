package com.splendor.project.domain.game.service;

import com.splendor.project.domain.data.GemType;
import com.splendor.project.domain.game.dto.request.DiscardTokenRequestDto;
import com.splendor.project.domain.game.dto.request.SelectStatus;
import com.splendor.project.domain.game.dto.request.SelectTokenRequestDto;
import com.splendor.project.domain.game.dto.response.*;
import com.splendor.project.domain.game.repository.GameStateRepository;
import com.splendor.project.domain.game.repository.SelectTokenStateRepository;
import com.splendor.project.domain.player.entity.Player;
import com.splendor.project.domain.room.entity.Room;
import com.splendor.project.domain.room.repository.RoomRepository;
import com.splendor.project.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.splendor.project.domain.data.GemType.*;
import static com.splendor.project.domain.game.dto.request.SelectStatus.IS_SELECT;

@Service
@RequiredArgsConstructor
@Transactional
public class PlayGameService {

    private final InitialGameService initialGameService;
    private final RoomRepository roomRepository;
    private final GameStateRepository gameStateRepository; // Redis Repository
    private final SelectTokenStateRepository selectTokenStateRepository; // Redis Repository
    private final TokenAcquisitionValidator tokenAcquisitionValidator; // 검증 서비스

    // =================================================================
    // 1. 초기화 로직
    // =================================================================

    public GameStateDto gameStart(Long roomId) {
        // 1. 보드 및 방 정보 초기화
        BoardStateDto boardStateDto = initialGameService.initializeGame();
        System.out.println("boardStateDto = " + boardStateDto);
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

    // =================================================================
    // 2. 토큰 선택 (중간 상태 관리) 로직
    // =================================================================

    /**
     * 플레이어가 토큰을 하나씩 선택/취소할 때마다 호출되는 중간 검증 및 상태 관리 로직.
     */
    public ResponseTokenDto selectToken(SelectTokenRequestDto request) {
        Long roomId = request.getRoomId();
        String senderId = request.getPlayerId();
        GemType token = request.getToken();
        SelectStatus selectStatus = request.getSelectStatus();

        GameStateDto gameStateDto = gameStateRepository.findById(roomId)
                .orElseThrow(() -> new NoSuchElementException(ErrorCode.ROOM_NOT_FOUND.getMessage()));

        // 턴 플레이어 검증 (보안 및 무결성 검사)
        if (!gameStateDto.getCurrentPlayer().getPlayerId().equals(senderId)) {
            throw new IllegalStateException("현재 턴이 아닙니다. 토큰을 선택할 수 없습니다.");
        }
        SelectTokenStateDto tokenStateDto = selectTokenStateRepository.findById(roomId).orElseThrow(() -> {
            throw new RuntimeException("Ff");
        });

        SelectTokenStateDto selectState = selectTokenStateRepository.findById(roomId)
                .orElseGet(() -> new SelectTokenStateDto(roomId, gameStateDto.getCurrentPlayer().getPlayerId()));


        Map<GemType, Integer> currentSelections = selectState.getTokensToTake();
        int currentCount = currentSelections.getOrDefault(token, 0);

        if (selectStatus.equals(IS_SELECT)) {
            Map<GemType, Integer> proposedSelections = new HashMap<>(currentSelections);
            proposedSelections.put(token, currentCount + 1);

            tokenAcquisitionValidator.validatePartialTokenAcquisition(proposedSelections, gameStateDto.getBoardStateDto().getAvailableTokens());

            // 검증 통과 시 상태 업데이트
            currentSelections.put(token, currentCount + 1);
        } else {
            // 토큰 '취소' 로직
            if (currentCount > 0) {
                currentSelections.put(token, currentCount - 1);
                if (currentSelections.get(token) == 0) {
                    currentSelections.remove(token);
                }
            }
        }
        selectTokenStateRepository.save(selectState);
        return new ResponseTokenDto(currentSelections);
    }


    // =================================================================
    // 3. 토큰 버리기 (10개 초과 시) 로직
    // =================================================================

    /**
     * 플레이어가 10개 초과 토큰을 버릴 때 호출되는 로직. (보유 토큰을 보드로 회수)
     */
    public GameStateDto discardToken(DiscardTokenRequestDto request) {
        Long gameId = request.getRoomId();
        String playerId = request.getPlayerId();
        GemType tokenToDiscard = request.getToken();

        GameStateDto gameStateDto = gameStateRepository.findById(gameId)
                .orElseThrow(() -> new NoSuchElementException(ErrorCode.ROOM_NOT_FOUND.getMessage()));

        // 턴 플레이어 검증 (보안 및 무결성 검사)
        if (!gameStateDto.getCurrentPlayer().getPlayerId().equals(playerId)) {
            throw new IllegalStateException("현재 턴이 아닙니다. 토큰을 버릴 수 없습니다.");
        }

        PlayerStateDto currentPlayerState = gameStateDto.getPlayerStateDto().stream()
                .filter(p -> p.getPlayer().getPlayerId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(ErrorCode.PLAYER_NOT_FOUND.getMessage()));

        Map<GemType, Integer> playerTokens = currentPlayerState.getTokens();
        Map<GemType, Integer> boardTokens = gameStateDto.getBoardStateDto().getAvailableTokens();

        int currentCount = playerTokens.getOrDefault(tokenToDiscard, 0);

        if (currentCount <= 0) {
            throw new IllegalArgumentException("버리려는 토큰(" + tokenToDiscard + ")을 플레이어가 소유하고 있지 않습니다.");
        }

        // 플레이어 토큰 감소 (버림)
        playerTokens.put(tokenToDiscard, currentCount - 1);
        if (playerTokens.get(tokenToDiscard) == 0) {
            playerTokens.remove(tokenToDiscard);
        }

        // 보드 토큰 증가 (보드로 회수)
        boardTokens.put(tokenToDiscard, boardTokens.getOrDefault(tokenToDiscard, 0) + 1);

        gameStateRepository.save(gameStateDto);
        return gameStateDto;
    }

    // =================================================================
    // 4. 턴 종료 로직
    // =================================================================

    /**
     * 현재 턴을 종료하고 다음 플레이어로 턴을 넘깁니다.
     * 선택된 토큰이 있으면 자동으로 획득(Commit)합니다. (프론트엔드 요구사항 반영)
     */
    public GameStateDto endTurn(Long roomId) {
        GameStateDto gameStateDto = gameStateRepository.findById(roomId)
                .orElseThrow(() -> new NoSuchElementException(ErrorCode.ROOM_NOT_FOUND.getMessage()));

        // 1. 임시 선택 상태 확인 및 처리
        Optional<SelectTokenStateDto> selectStateOpt = selectTokenStateRepository.findById(roomId);

        if (selectStateOpt.isPresent()) {
            Map<GemType, Integer> tokensToAcquire = selectStateOpt.get().getTokensToTake();
            String playerId = gameStateDto.getCurrentPlayer().getPlayerId();

            if (!tokensToAcquire.isEmpty() && tokensToAcquire.values().stream().mapToInt(Integer::intValue).sum() > 0) {

                tokenAcquisitionValidator.validateTokenAcquisition(tokensToAcquire, gameStateDto.getBoardStateDto().getAvailableTokens());

                // 1.2. 플레이어 상태 찾기
                PlayerStateDto currentPlayerState = gameStateDto.getPlayerStateDto().stream()
                        .filter(p -> p.getPlayer().getPlayerId().equals(playerId))
                        .findFirst()
                        .orElseThrow(() -> new NoSuchElementException(ErrorCode.PLAYER_NOT_FOUND.getMessage()));

                // 1.3. 영구 상태 업데이트 (보드 토큰 감소, 플레이어 토큰 증가)
                updateBoardTokens(tokensToAcquire, gameStateDto.getBoardStateDto().getAvailableTokens());
                updatePlayerTokens(tokensToAcquire, currentPlayerState.getTokens());
            }
        }

        // 2. 임시 선택 상태 삭제 (획득 여부와 관계없이 정리)
        selectTokenStateRepository.deleteById(roomId);

        // 3. 다음 플레이어로 턴 변경 로직 (기존 endTurn 로직)
        List<PlayerStateDto> players = gameStateDto.getPlayerStateDto();
        GamePlayerDto currentPlayer = gameStateDto.getCurrentPlayer();

        int currentIndex = -1;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getPlayer().getPlayerId().equals(currentPlayer.getPlayerId())) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex == -1) {
            throw new IllegalStateException("현재 턴 플레이어를 찾을 수 없습니다.");
        }

        int nextIndex = (currentIndex + 1) % players.size();
        GamePlayerDto nextPlayer = players.get(nextIndex).getPlayer();

        // GameStateDto의 현재 플레이어를 업데이트합니다.
        gameStateDto.setCurrentPlayer(nextPlayer);

        // 4. Redis에 업데이트된 게임 상태 저장
        gameStateRepository.save(gameStateDto);
        return gameStateDto;
    }

    // =================================================================
    // 5. 상태 업데이트 헬퍼 메서드 (검증 메서드는 Validator 클래스로 이동)
    // =================================================================

    // 보드 토큰 업데이트 (감소)
    private void updateBoardTokens(Map<GemType, Integer> tokensToTake, Map<GemType, Integer> availableTokens) {
        for (Map.Entry<GemType, Integer> entry : tokensToTake.entrySet()) {
            GemType gemType = entry.getKey();
            int count = entry.getValue();
            availableTokens.put(gemType, availableTokens.getOrDefault(gemType, 0) - count);
        }
    }

    // 플레이어 토큰 업데이트 (증가)
    private void updatePlayerTokens(Map<GemType, Integer> tokensToTake, Map<GemType, Integer> playerTokens) {
        for (Map.Entry<GemType, Integer> entry : tokensToTake.entrySet()) {
            GemType gemType = entry.getKey();
            int count = entry.getValue();
            playerTokens.put(gemType, playerTokens.getOrDefault(gemType, 0) + count);
        }
    }
}