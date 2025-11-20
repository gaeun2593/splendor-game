package com.splendor.project.domain.game.service;

import com.splendor.project.domain.data.GemType;
import com.splendor.project.domain.game.dto.request.SelectTokenRequestDto;
import com.splendor.project.domain.game.dto.request.TakeTokenRequestDto;
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

    /**
     * 플레이어가 토큰을 하나씩 선택/취소할 때마다 호출되는 중간 검증 및 상태 관리 로직.
     * @param request 토큰 선택 요청 (어떤 토큰을 선택/취소했는지)
     * @return 현재까지 선택된 토큰 목록 및 수량 (Map<GemType, Integer>)
     */
    public Map<GemType, Integer> selectToken(SelectTokenRequestDto request) {
        Long roomId = request.getRoomId();
        GemType token = request.getToken();
        boolean isSelected = request.isSelected();

        // 1. 현재 게임 상태 및 선택 상태 로드
        GameStateDto gameStateDto = gameStateRepository.findById(roomId)
                .orElseThrow(() -> new NoSuchElementException(ErrorCode.ROOM_NOT_FOUND.getMessage()));

        // 1.1. 턴 플레이어 검증
        if (!gameStateDto.getCurrentPlayer().getPlayerId().equals(request.getCurrentTurnId())) {
            throw new IllegalStateException("현재 턴이 아닙니다. 토큰을 선택할 수 없습니다.");
        }

        SelectTokenStateDto selectState = selectTokenStateRepository.findById(roomId)
                .orElseGet(() -> new SelectTokenStateDto(roomId, request.getCurrentTurnId()));

        Map<GemType, Integer> currentSelections = selectState.getTokensToTake();
        int currentCount = currentSelections.getOrDefault(token, 0);

        if (isSelected) {
            // 토큰 '선택' 로직
            Map<GemType, Integer> proposedSelections = new HashMap<>(currentSelections);
            proposedSelections.put(token, currentCount + 1);

            // 2. 선택 추가 시 검증 로직
            validatePartialTokenAcquisition(proposedSelections, gameStateDto.getBoardStateDto().getAvailableTokens());

            // 3. 검증 통과 시 상태 업데이트 및 저장
            currentSelections.put(token, currentCount + 1);
        } else {
            // 토큰 '취소' 로직
            if (currentCount > 0) {
                currentSelections.put(token, currentCount - 1);
                if (currentSelections.get(token) == 0) {
                    currentSelections.remove(token);
                }
            }
            // 취소는 검증이 필요 없으나, 상태는 저장해야 함
        }

        // 4. Redis에 선택 상태 저장
        selectTokenStateRepository.save(selectState);

        return currentSelections;
    }

    /**
     * 토큰이 하나씩 추가될 때마다 현재까지 선택된 토큰 목록의 유효성을 검사합니다.
     * @param tokensToTake 현재까지 선택된 토큰 맵
     * @param availableTokens 보드에 남아있는 토큰 맵
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
     * 플레이어가 보드에서 보석 토큰을 획득하는 메인 로직.
     * @param request 토큰 획득 요청 정보 (roomId, playerId, tokensToTake)
     * @return 업데이트된 게임 상태 DTO
     */
    public GameStateDto takeTokens(TakeTokenRequestDto request) {
        Long gameId = request.getRoomId();
        String playerId = request.getPlayerId();
        Map<GemType, Integer> tokensToTake = request.getTokensToTake();

        // 1. Redis에서 현재 게임 상태 로드
        GameStateDto gameStateDto = gameStateRepository.findById(gameId)
                .orElseThrow(() -> new NoSuchElementException(ErrorCode.ROOM_NOT_FOUND.getMessage()));

        // 2. 획득 요청의 유효성 검증
        validateTokenAcquisition(tokensToTake, gameStateDto.getBoardStateDto().getAvailableTokens());

        // 3. 플레이어 상태 찾기
        PlayerStateDto currentPlayerState = gameStateDto.getPlayerStateDto().stream()
                .filter(p -> p.getPlayer().getPlayerId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(ErrorCode.PLAYER_NOT_FOUND.getMessage()));

        // 4-1. 보드 토큰 업데이트 (상태)
        updateBoardTokens(tokensToTake, gameStateDto.getBoardStateDto().getAvailableTokens());

        // 4-2. 플레이어 토큰 업데이트 (상태)
        updatePlayerTokens(tokensToTake, currentPlayerState.getTokens());

        // 5. 플레이어 최대 토큰 개수(10개) 검증 (사용자 요구 조건)
        int totalPlayerTokens = currentPlayerState.getTokens().values().stream().mapToInt(Integer::intValue).sum();
        if (totalPlayerTokens > 10) {
            // 토큰이 10개를 초과하면, 플레이어는 버리는(discard) 후속 행동을 해야 함
            // 현재는 초과 상태를 알리고 예외를 발생
            throw new IllegalStateException(ErrorCode.PLAYER_TOKEN_LIMIT_EXCEEDED.getMessage());
        }

        // 6. 업데이트된 게임 상태 Redis에 저장
        gameStateRepository.save(gameStateDto);

        // 7. 다음 턴 로직은 이 메서드가 끝난 후 별도의 서비스나 컨트롤러에서 처리해야 함

        return gameStateDto;
    }

    // ========== 토큰 획득 검증 로직 (최종 액션용) ==========
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

    // ========== 보드 토큰 업데이트 로직 ==========

    private void updateBoardTokens(Map<GemType, Integer> tokensToTake, Map<GemType, Integer> availableTokens) {
        // availableTokens는 BoardStateDto에 속한 Map 객체이므로, 변경하면 BoardStateDto에 반영됩니다.
        for (Map.Entry<GemType, Integer> entry : tokensToTake.entrySet()) {
            GemType gemType = entry.getKey();
            int count = entry.getValue();

            // 기존 맵을 직접 수정합니다. (Redis에 DTO 전체를 다시 저장하므로 문제 없음)
            availableTokens.put(gemType, availableTokens.getOrDefault(gemType, 0) - count);
        }
    }

    // ========== 플레이어 토큰 업데이트 로직 ==========

    private void updatePlayerTokens(Map<GemType, Integer> tokensToTake, Map<GemType, Integer> playerTokens) {
        // playerTokens는 PlayerStateDto에 속한 Map 객체이므로, 변경하면 PlayerStateDto에 반영됩니다.
        for (Map.Entry<GemType, Integer> entry : tokensToTake.entrySet()) {
            GemType gemType = entry.getKey();
            int count = entry.getValue();

            // 기존 맵을 직접 수정합니다.
            playerTokens.put(gemType, playerTokens.getOrDefault(gemType, 0) + count);
        }
    }
}