package com.splendor.project.domain.game.service;

import com.splendor.project.domain.data.GemType;
import com.splendor.project.domain.data.StaticCard;
import com.splendor.project.domain.game.dto.request.DiscardTokenRequestDto;
import com.splendor.project.domain.game.dto.request.SelectStatus;
import com.splendor.project.domain.game.dto.request.SelectTokenRequestDto;
import com.splendor.project.domain.game.dto.request.SelectCardRequestDto;
import com.splendor.project.domain.game.dto.response.*;
import com.splendor.project.domain.game.logic.PlayerStateCalculator;
import com.splendor.project.domain.game.repository.SelectionCardStateRepository;
import com.splendor.project.domain.game.repository.GameStateRepository;
import com.splendor.project.domain.game.repository.SelectTokenStateRepository;
import com.splendor.project.domain.player.entity.Player;
import com.splendor.project.domain.room.entity.Room;
import com.splendor.project.domain.room.repository.RoomRepository;
import com.splendor.project.exception.ErrorCode;
import com.splendor.project.exception.GameLogicException;
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
    private final GameStateRepository gameStateRepository;
    private final SelectTokenStateRepository selectTokenStateRepository;
    private final SelectionCardStateRepository cardSelectionStateRepository;
    private final TokenAcquisitionValidator tokenAcquisitionValidator;
    private final GameStaticDataLoader staticDataLoader;

    // =================================================================
    // 1. 초기화 로직
    // =================================================================

    public GameStateDto gameStart(Long roomId) {
        BoardStateDto boardStateDto = initialGameService.initializeGame();
        System.out.println("boardStateDto = " + boardStateDto);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new GameLogicException(ErrorCode.ROOM_NOT_FOUND));

        List<Player> players = room.getPlayers();
        Collections.shuffle(players);

        Player startingPlayer = players.get(0);
        GamePlayerDto gamePlayerDto = new GamePlayerDto(startingPlayer.getNickname(), startingPlayer.getPlayerId());

        List<PlayerStateDto> playerStateDtos = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            playerStateDtos.add(new PlayerStateDto(
                    new GamePlayerDto(player.getNickname(), player.getPlayerId()),
                    0,
                    Map.of(DIAMOND, 0, RUBY, 0, EMERALD, 0, SAPPHIRE, 0, ONYX, 0, GOLD, 0),
                    Map.of(DIAMOND, 0, RUBY, 0, EMERALD, 0, SAPPHIRE, 0, ONYX, 0, GOLD, 0),
                    0,
                    0,
                    i  // turnOrder 초기화
            ));
        }

        GameStateDto gameStateDto = new GameStateDto(
                boardStateDto,
                playerStateDtos,
                room.getRoomId(),
                gamePlayerDto,
                false,
                null,
                false,
                startingPlayer.getPlayerId()
        );

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
                .orElseThrow(() -> new GameLogicException(ErrorCode.ROOM_NOT_FOUND));

        if (!gameStateDto.getCurrentPlayer().getPlayerId().equals(senderId)) {
            throw new GameLogicException(ErrorCode.NOT_CURRENT_TURN);
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
                .orElseThrow(() -> new GameLogicException(ErrorCode.ROOM_NOT_FOUND));

        // 턴 플레이어 검증 (보안 및 무결성 검사)
        if (!gameStateDto.getCurrentPlayer().getPlayerId().equals(playerId)) {
            throw new GameLogicException(ErrorCode.NOT_CURRENT_TURN);
        }

        PlayerStateDto currentPlayerState = gameStateDto.getPlayerStateDto().stream()
                .filter(p -> p.getPlayer().getPlayerId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new GameLogicException(ErrorCode.PLAYER_NOT_FOUND));

        Map<GemType, Integer> playerTokens = currentPlayerState.getTokens();
        Map<GemType, Integer> boardTokens = gameStateDto.getBoardStateDto().getAvailableTokens();

        int currentCount = playerTokens.getOrDefault(tokenToDiscard, 0);

        if (currentCount <= 0) {
            throw new GameLogicException(ErrorCode.INVALID_TOKEN_ACTION);
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
    // 4. 카드 선택/취소 (중간 상태 관리) 로직
    // =================================================================

    /**
     * 플레이어가 구매할 카드를 선택하거나 선택을 취소하는 로직.
     */
    public SelectionCardStateDto selectCard(SelectCardRequestDto request) {
        Long roomId = request.getRoomId();
        String playerId = request.getPlayerId();
        int cardId = request.getCardId();

        GameStateDto gameStateDto = gameStateRepository.findById(roomId)
                .orElseThrow(() -> new GameLogicException(ErrorCode.ROOM_NOT_FOUND));

        if (!gameStateDto.getCurrentPlayer().getPlayerId().equals(playerId)) {
            throw new GameLogicException(ErrorCode.NOT_CURRENT_TURN);
        }

        SelectionCardStateDto selectionState = cardSelectionStateRepository.findById(roomId)
                .orElseGet(() -> new SelectionCardStateDto(roomId, playerId));

        // 다른 행동(토큰 선택)이 있는지 확인
        if (selectTokenStateRepository.findById(roomId).map(SelectTokenStateDto::getTokensToTake).orElse(Collections.emptyMap()).size() > 0) {
            throw new GameLogicException(ErrorCode.INVALID_TOKEN_ACTION); // 이미 토큰을 선택함
        }


        if (request.isSelected()) {
            // 1. 카드가 존재하는지 확인
            if (staticDataLoader.getAllCards().stream().noneMatch(c -> c.id() == cardId)) {
                throw new GameLogicException(ErrorCode.CARD_NOT_AVAILABLE);
            }

            // 2. 이미 카드가 선택된 경우
            if (selectionState.getCardIdToBuy() != null) {
                if (selectionState.getCardIdToBuy() == cardId) {
                    // 같은 카드를 다시 선택: 이미 선택됨
                    throw new GameLogicException(ErrorCode.ANOTHER_CARD_ALREADY_SELECTED);
                } else {
                    // 다른 카드를 선택: 이미 다른 카드가 선택됨
                    throw new GameLogicException(ErrorCode.ANOTHER_CARD_ALREADY_SELECTED);
                }
            }

            // 3. 카드 선택 (상태 저장)
            selectionState.setCardIdToBuy(cardId);

        } else {
            // 카드 취소 요청
            if (selectionState.getCardIdToBuy() == null || selectionState.getCardIdToBuy() != cardId) {
                throw new GameLogicException(ErrorCode.INVALID_TOKEN_ACTION); // 취소할 카드가 없거나 다른 카드를 취소 시도
            }
            selectionState.setCardIdToBuy(null);
        }

        cardSelectionStateRepository.save(selectionState);
        return selectionState;
    }


    // =================================================================
    // 5. 턴 종료 로직 (토큰 획득/카드 구매 커밋)
    // =================================================================

    /**
     * 현재 턴을 종료하고 다음 플레이어로 턴을 넘깁니다. (유효한 행동만 커밋)
     */
    public GameStateDto endTurn(Long roomId) {
        GameStateDto gameStateDto = gameStateRepository.findById(roomId)
                .orElseThrow(() -> new GameLogicException(ErrorCode.ROOM_NOT_FOUND));

        String playerId = gameStateDto.getCurrentPlayer().getPlayerId();

        Optional<SelectionCardStateDto> selectionStateOpt = cardSelectionStateRepository.findById(roomId);
        Optional<SelectTokenStateDto> selectStateOpt = selectTokenStateRepository.findById(roomId);

        boolean cardPurchaseAttempted = selectionStateOpt.isPresent() && selectionStateOpt.get().getCardIdToBuy() != null;
        boolean tokenAcquisitionAttempted = selectStateOpt.isPresent() && selectStateOpt.get().getTokensToTake().values().stream().mapToInt(Integer::intValue).sum() > 0;

        // 행동 타입 결정 및 실행
        if (cardPurchaseAttempted) {
            // 카드 구매 액션 실행 (Commit)
            commitCardPurchase(roomId, gameStateDto, selectionStateOpt.get());
            cardSelectionStateRepository.deleteById(roomId);

            // 💡 토큰 선택 상태는 카드 구매 시 자동으로 무시되므로 정리
            selectStateOpt.ifPresent(state -> selectTokenStateRepository.deleteById(roomId));

        } else if (tokenAcquisitionAttempted) {
            // 토큰 획득 액션 실행 (Commit)
            commitTokenAcquisition(roomId, gameStateDto, selectStateOpt.get());
            selectTokenStateRepository.deleteById(roomId);

            // 💡 카드 선택 상태는 토큰 획득 시 자동으로 무시되므로 정리
            selectionStateOpt.ifPresent(state -> cardSelectionStateRepository.deleteById(roomId));

        } else {
            // 아무 행동도 하지 않은 경우 (Pass) - 임시 상태 정리만 수행
            selectionStateOpt.ifPresent(state -> cardSelectionStateRepository.deleteById(roomId));
            selectStateOpt.ifPresent(state -> selectTokenStateRepository.deleteById(roomId));
        }

        // 1. 점수 체크 및 최종 라운드 시작 플래그 설정 (점수 15점 이상 체크)
        checkGameEndCondition(gameStateDto);

        // 2. 다음 플레이어로 턴 변경 로직 (항상 실행되어야 함)
        advanceTurn(gameStateDto);

        // 3. 게임 종료 조건 확인 (턴을 받은 플레이어(currentPlayer)가 시작 플레이어인지 확인)
        if (gameStateDto.isFinalRound() && isCurrentPlayerStartingPlayer(gameStateDto)) {
            // 게임 종료: 라운드 종료
            GamePlayerDto winner = determineWinner(gameStateDto);

            // 최종 상태 DTO에 결과 기록
            gameStateDto.setGameOver(true);
            gameStateDto.setWinner(winner);

            // Redis 정리 및 최종 DTO 반환 (저장 불필요)
            gameStateRepository.deleteById(roomId);
            return gameStateDto;
        }

        // 4. 게임이 계속되는 경우: Redis에 업데이트된 게임 상태 저장
        gameStateRepository.save(gameStateDto);
        return gameStateDto;
    }

    // =================================================================
    // 6. 커밋 헬퍼 메서드
    // =================================================================

    private void commitCardPurchase(Long roomId, GameStateDto gameStateDto, SelectionCardStateDto selectionState) {
        int cardId = selectionState.getCardIdToBuy();
        String playerId = selectionState.getPlayerId();

        PlayerStateDto currentPlayerState = gameStateDto.getPlayerStateDto().stream()
                .filter(p -> p.getPlayer().getPlayerId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new GameLogicException(ErrorCode.PLAYER_NOT_FOUND));

        StaticCard cardToBuy = staticDataLoader.getAllCards().stream()
                .filter(card -> card.id() == cardId)
                .findFirst()
                .orElseThrow(() -> new GameLogicException(ErrorCode.CARD_NOT_AVAILABLE));

        // 💡 최종 검증 및 확정적 지불 비용 계산
        Map<GemType, Integer> finalPayment = PlayerStateCalculator.calculatePayment(currentPlayerState, cardToBuy);

        // 상태 변경
        updatePlayerStateAfterPurchase(currentPlayerState, cardToBuy, finalPayment);
        updateBoardStateAfterPurchase(gameStateDto.getBoardStateDto(), cardToBuy, finalPayment);

        // 카드 카운트 업데이트
        currentPlayerState.setPurchasedCardCount(currentPlayerState.getPurchasedCardCount() + 1);
    }

    private void commitTokenAcquisition(Long roomId, GameStateDto gameStateDto, SelectTokenStateDto selectState) {
        Map<GemType, Integer> tokensToAcquire = selectState.getTokensToTake();
        String playerId = gameStateDto.getCurrentPlayer().getPlayerId();

        tokenAcquisitionValidator.validateTokenAcquisition(tokensToAcquire, gameStateDto.getBoardStateDto().getAvailableTokens());

        PlayerStateDto currentPlayerState = gameStateDto.getPlayerStateDto().stream()
                .filter(p -> p.getPlayer().getPlayerId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new GameLogicException(ErrorCode.PLAYER_NOT_FOUND));

        updateBoardTokens(tokensToAcquire, gameStateDto.getBoardStateDto().getAvailableTokens());
        updatePlayerTokens(tokensToAcquire, currentPlayerState.getTokens());
    }

    // =================================================================
    // 7. 헬퍼 메서드
    // =================================================================

    // 턴을 다음 플레이어로 넘기는 공통 로직
    private void advanceTurn(GameStateDto gameStateDto) {
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
            throw new GameLogicException(ErrorCode.PLAYER_NOT_FOUND);
        }

        int nextIndex = (currentIndex + 1) % players.size();
        GamePlayerDto nextPlayer = players.get(nextIndex).getPlayer();

        gameStateDto.setCurrentPlayer(nextPlayer);
    }

    // 카드 구매 후 플레이어 상태 업데이트
    private void updatePlayerStateAfterPurchase(
            PlayerStateDto playerState, StaticCard card, Map<GemType, Integer> payment) {

        Map<GemType, Integer> playerTokens = playerState.getTokens();
        for (Map.Entry<GemType, Integer> entry : payment.entrySet()) {
            GemType gem = entry.getKey();
            int paidCount = entry.getValue();
            playerTokens.put(gem, playerTokens.getOrDefault(gem, 0) - paidCount);
            if (playerTokens.get(gem) <= 0) {
                playerTokens.remove(gem);
            }
        }

        Map<GemType, Integer> playerBonuses = playerState.getBonuses();
        playerBonuses.merge(card.bonusGem(), 1, Integer::sum);

        playerState.setScore(playerState.getScore() + card.points());
    }

    // 카드 구매 후 보드 상태 업데이트
    private void updateBoardStateAfterPurchase(
            BoardStateDto boardState, StaticCard purchasedCard, Map<GemType, Integer> returnedTokens) {

        Map<GemType, Integer> availableTokens = boardState.getAvailableTokens();
        for (Map.Entry<GemType, Integer> entry : returnedTokens.entrySet()) {
            availableTokens.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }

        List<List<StaticCard>> cardsByLevel = boardState.getCards();
        int levelIndex = purchasedCard.level() - 1;

        if (levelIndex >= 0 && levelIndex < cardsByLevel.size()) {
            List<StaticCard> levelCards = cardsByLevel.get(levelIndex);

            boolean removed = levelCards.remove(purchasedCard);

            if (removed) {
                // TODO: 덱에서 새 카드를 뽑아와 levelCards에 추가하는 **보충** 로직 구현 필요
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

    // 턴을 받은 플레이어(currentPlayer)가 시작 플레이어인지 확인
    private boolean isCurrentPlayerStartingPlayer(GameStateDto gameStateDto) {
        return gameStateDto.getCurrentPlayer().getPlayerId().equals(gameStateDto.getStartingPlayerId());
    }

    // 점수 15점 이상 달성 시 isFinalRound 플래그 설정
    private void checkGameEndCondition(GameStateDto gameStateDto) {
        if (gameStateDto.isFinalRound()) {
            return; // 이미 최종 라운드가 시작됨
        }

        boolean scoreMet = gameStateDto.getPlayerStateDto().stream()
                .anyMatch(playerState -> playerState.getScore() >= 15);

        if (scoreMet) {
            gameStateDto.setFinalRound(true);
            System.out.println("15점 이상 달성! 최종 라운드가 시작됩니다.");
        }
    }

    // 최종 승자 결정 로직 (규칙 5, 6 반영)
    private GamePlayerDto determineWinner(GameStateDto gameStateDto) {
        List<PlayerStateDto> players = gameStateDto.getPlayerStateDto();

        Optional<PlayerStateDto> winnerState = players.stream()
                .max(Comparator
                        // 1. 점수가 높은 사람 (규칙 5)
                        .comparing(PlayerStateDto::getScore)
                        // 2. 개발 카드 수가 더 적은 사람 (규칙 6-1)
                        .thenComparing(player -> player.getPurchasedCardCount() * -1) // * -1을 곱하여 '적은' 사람이 높은 순위가 되도록 반전
                        // 3. 귀족 카드를 더 많이 가지고 있는 사람 (규칙 6-2)
                        .thenComparing(PlayerStateDto::getNobleCount)
                        // 4. 남은 보석 토큰의 수가 더 많은 사람 (규칙 6-3)
                        .thenComparing(player -> player.getTokens().values().stream().mapToInt(Integer::intValue).sum())
                        // 5. 후공 플레이어의 승리 (턴 순서 인덱스가 더 큰 사람) (규칙 6-4)
                        .thenComparing(PlayerStateDto::getTurnOrder)
                );

        return winnerState.map(PlayerStateDto::getPlayer).orElse(null);
    }
}