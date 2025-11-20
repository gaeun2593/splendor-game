package com.splendor.project.domain.game.service;

import com.splendor.project.domain.data.GemType;
import com.splendor.project.domain.game.dto.request.DiscardTokenRequestDto;
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
import java.util.stream.Collectors;

import static com.splendor.project.domain.data.GemType.*;

@Service
@RequiredArgsConstructor
@Transactional
public class PlayGameService {

    private final InitialGameService initialGameService;
    private final RoomRepository roomRepository;
    private final GameStateRepository gameStateRepository; // Redis Repository
    private final SelectTokenStateRepository selectTokenStateRepository; // Redis Repository

    // =================================================================
    // 1. 초기화 로직
    // =================================================================

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

    // =================================================================
    // 2. 토큰 선택 (중간 상태 관리) 로직
    // =================================================================

    /**
     * 플레이어가 토큰을 하나씩 선택/취소할 때마다 호출되는 중간 검증 및 상태 관리 로직.
     */
    public Map<GemType, Integer> selectToken(SelectTokenRequestDto request) {
        Long roomId = request.getRoomId();
        GemType token = request.getToken();
        boolean isSelected = request.isSelected();

        GameStateDto gameStateDto = gameStateRepository.findById(roomId)
                .orElseThrow(() -> new NoSuchElementException(ErrorCode.ROOM_NOT_FOUND.getMessage()));

        // 턴 플레이어 검증
        if (!gameStateDto.getCurrentPlayer().getPlayerId().equals(request.getCurrentTurnId())) {
            throw new IllegalStateException("현재 턴이 아닙니다. 토큰을 선택할 수 없습니다.");
        }

        SelectTokenStateDto selectState = selectTokenStateRepository.findById(roomId)
                .orElseGet(() -> new SelectTokenStateDto(roomId, request.getCurrentTurnId()));

        Map<GemType, Integer> currentSelections = selectState.getTokensToTake();
        int currentCount = currentSelections.getOrDefault(token, 0);

        if (isSelected) {
            Map<GemType, Integer> proposedSelections = new HashMap<>(currentSelections);
            proposedSelections.put(token, currentCount + 1);

            // 선택 추가 시 검증 로직
            validatePartialTokenAcquisition(proposedSelections, gameStateDto.getBoardStateDto().getAvailableTokens());

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
        return currentSelections;
    }


    // =================================================================
    // 3. 토큰 버리기 (10개 초과 시) 로직
    // =================================================================

    /**
     * 플레이어가 10개 초과 토큰을 버릴 때 호출되는 로직. (보유 토큰을 보드로 회수)
     */
    public GameStateDto discardToken(DiscardTokenRequestDto request) {
        Long gameId = request.getRoomId();
        String playerId = request.getCurrentTurnId();
        GemType tokenToDiscard = request.getToken();

        GameStateDto gameStateDto = gameStateRepository.findById(gameId)
                .orElseThrow(() -> new NoSuchElementException(ErrorCode.ROOM_NOT_FOUND.getMessage()));

        // 턴 플레이어 검증
        if (!gameStateDto.getCurrentPlayer().getPlayerId().equals(request.getCurrentTurnId())) {
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

            // 획득할 토큰이 실제로 존재하는 경우에만 커밋 진행
            if (!tokensToAcquire.isEmpty() && tokensToAcquire.values().stream().mapToInt(Integer::intValue).sum() > 0) {

                // 1.1. 최종 획득 규칙 검증 (Map 형식)
                validateTokenAcquisition(tokensToAcquire, gameStateDto.getBoardStateDto().getAvailableTokens());

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
        selectTokenStateRepository.deleteById(roomId); // 기존 위치보다 위로 이동/재확인

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
    // 5. 검증 및 상태 업데이트 헬퍼 메서드
    // =================================================================

    /**
     * 토큰이 하나씩 추가될 때마다 현재까지 선택된 토큰 목록의 유효성을 검사합니다. (부분 검증)
     */
    private void validatePartialTokenAcquisition(Map<GemType, Integer> tokensToTake, Map<GemType, Integer> availableTokens) {
        // 0. 골드 토큰 요청 검증 (일반 획득 시 선택 불가)
        if (tokensToTake.containsKey(GOLD) && tokensToTake.get(GOLD) > 0) {
            throw new IllegalArgumentException(ErrorCode.INVALID_TOKEN_ACTION.getMessage() + " (골드 토큰은 일반 획득 시 선택 불가)");
        }

        // 유효한 선택만 필터링 (개수 0인 토큰 제외)
        Map<GemType, Integer> validTokensToTake = tokensToTake.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // 1. 총 개수 3개 초과 검증
        int totalTakeCount = validTokensToTake.values().stream().mapToInt(Integer::intValue).sum();
        if (totalTakeCount > 3) {
            throw new IllegalArgumentException(ErrorCode.INVALID_TOKEN_ACTION.getMessage() + " (최대 3개 초과)");
        }

        // 2. 종류 개수 검증 (최대 3종류)
        int distinctGemCount = validTokensToTake.size();
        if (distinctGemCount > 3) {
            throw new IllegalArgumentException(ErrorCode.TOO_MANY_TOKEN_TYPES.getMessage());
        }

        // 3. 2개 획득 규칙 검증
        if (distinctGemCount == 1) {
            if (totalTakeCount == 2) {
                GemType gemType = validTokensToTake.keySet().iterator().next();
                Integer availableCount = availableTokens.getOrDefault(gemType, 0);

                // 같은 보석 2개 획득 시 4개 이상 남아있어야 함
                if (availableCount < 4) {
                    throw new IllegalArgumentException(ErrorCode.INVALID_TWO_TOKEN_RULE.getMessage());
                }
            } else if (totalTakeCount > 2) {
                // 한 종류를 3개 이상 선택 시도 (불가능한 행동)
                throw new IllegalArgumentException(ErrorCode.INVALID_TOKEN_ACTION.getMessage() + " (한 종류 3개 이상 선택 불가)");
            }
        } else if (distinctGemCount >= 2) {
            // 4. 서로 다른 종류 토큰 획득 시 개수 검증 (각각 1개만 가능)
            if (validTokensToTake.values().stream().anyMatch(count -> count > 1)) {
                throw new IllegalArgumentException(ErrorCode.INVALID_TOKEN_ACTION.getMessage() + " (여러 종류 획득 시 각 1개 제한)");
            }
        }

        // 5. 보드 재고 검증
        for (Map.Entry<GemType, Integer> entry : validTokensToTake.entrySet()) {
            GemType gemType = entry.getKey();
            int count = entry.getValue();

            Integer availableCount = availableTokens.getOrDefault(gemType, 0);
            if (availableCount < count) {
                throw new IllegalArgumentException(ErrorCode.NOT_ENOUGH_BOARD_TOKEN.getMessage() + " (" + gemType + " 부족)");
            }
        }
    }

    /**
     * 토큰 획득 요청 DTO에 대한 최종 검증 로직. (takeTokens 전 호출)
     */
    private void validateTokenAcquisition(Map<GemType, Integer> tokensToTake, Map<GemType, Integer> availableTokens) {
        // 0. 가져갈 수 없는 골드 토큰 요청 제외
        if (tokensToTake.containsKey(GOLD) && tokensToTake.get(GOLD) > 0) {
            throw new IllegalArgumentException(ErrorCode.INVALID_TOKEN_ACTION.getMessage());
        }

        // 가져가려는 보석 종류만 필터링
        Map<GemType, Integer> validTokensToTake = tokensToTake.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        int totalTakeCount = validTokensToTake.values().stream().mapToInt(Integer::intValue).sum();
        int distinctGemCount = validTokensToTake.size();

        // 1. 가져가려는 토큰의 총 개수 확인 (스플렌더 기본 룰: 최대 3개)
        if (totalTakeCount > 3 || totalTakeCount <= 0) {
            throw new IllegalArgumentException(ErrorCode.INVALID_TOKEN_ACTION.getMessage() + " (총 개수)");
        }

        // 2. 획득 행동 유형 검증
        if (distinctGemCount == 1) {
            // A. 한 종류 2개 획득 (totalTakeCount = 2)
            if (totalTakeCount == 2) {
                GemType gemType = validTokensToTake.keySet().iterator().next();
                Integer availableCount = availableTokens.getOrDefault(gemType, 0);

                // [사용자 요구 규칙] 같은 보석 2개 획득 시 4개 이상 남아있어야 함
                if (availableCount < 4) {
                    throw new IllegalArgumentException(ErrorCode.INVALID_TWO_TOKEN_RULE.getMessage());
                }
            } else if (totalTakeCount == 3) {
                // B. 한 종류 3개 획득 요청 (불가능한 행동)
                throw new IllegalArgumentException(ErrorCode.INVALID_TOKEN_ACTION.getMessage() + " (한 종류 3개)");
            }
            // C. 한 종류 1개 획득은 항상 유효 (totalTakeCount = 1)

        } else if (distinctGemCount >= 2 && distinctGemCount <= 3) {
            // D. 서로 다른 종류 토큰 획득 (스플렌더 기본 룰: 최대 3종류, 각 1개씩)
            if (totalTakeCount != distinctGemCount) {
                // 2종류 이상 가져가는데, 각 1개가 아니면 (예: {D:2, S:1}) 유효하지 않음.
                throw new IllegalArgumentException(ErrorCode.INVALID_TOKEN_ACTION.getMessage() + " (여러 종류 획득 시 개수)");
            }
        } else {
            // 4종류 이상을 시도하거나, 0종류를 시도하는 경우 (이미 validTokensToTake에서 필터링되었지만 안전망)
            throw new IllegalArgumentException(ErrorCode.TOO_MANY_TOKEN_TYPES.getMessage());
        }

        // 3. 보드 재고 검증
        for (Map.Entry<GemType, Integer> entry : validTokensToTake.entrySet()) {
            GemType gemType = entry.getKey();
            int count = entry.getValue();

            Integer availableCount = availableTokens.getOrDefault(gemType, 0);
            if (availableCount < count) {
                throw new IllegalArgumentException(ErrorCode.NOT_ENOUGH_BOARD_TOKEN.getMessage() + " (" + gemType + " 부족)");
            }
        }
    }

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