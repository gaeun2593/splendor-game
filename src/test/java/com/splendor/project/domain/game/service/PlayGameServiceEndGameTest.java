package com.splendor.project.domain.game.service;

import com.splendor.project.domain.data.GemType;
import com.splendor.project.domain.game.dto.response.BoardStateDto;
import com.splendor.project.domain.game.dto.response.GamePlayerDto;
import com.splendor.project.domain.game.dto.response.GameStateDto;
import com.splendor.project.domain.game.dto.response.PlayerStateDto;
import com.splendor.project.domain.game.repository.GameStateRepository;
import com.splendor.project.domain.game.repository.SelectTokenStateRepository;
import com.splendor.project.domain.game.repository.SelectionCardStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.splendor.project.domain.data.GemType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayGameServiceEndGameTest {

    @Mock
    private GameStateRepository gameStateRepository;
    @Mock
    private SelectTokenStateRepository selectTokenStateRepository;
    @Mock
    private SelectionCardStateRepository cardSelectionStateRepository;
    @Mock
    private TokenAcquisitionValidator tokenAcquisitionValidator;
    @InjectMocks
    private PlayGameService playGameService;

    private final Long TEST_ROOM_ID = 1L;
    private final String P1_ID = "player-1"; // 선 플레이어
    private final String P2_ID = "player-2";
    private final String P3_ID = "player-3";

    private List<PlayerStateDto> threePlayerStates;
    private GameStateDto initialGameState;

    // 테스트용 PlayerStateDto를 생성하는 헬퍼 메서드
    private PlayerStateDto createPlayerState(
            String id, String name, int score, int cardCount, int nobleCount, int tokenCount, int turnOrder) {

        GamePlayerDto player = new GamePlayerDto(name, id);

        // 토큰 합계를 tokenCount로 맞추기 위한 단순화
        Map<GemType, Integer> tokens = new HashMap<>();
        if (tokenCount > 0) {
            tokens.put(DIAMOND, tokenCount);
        } else {
            // 모든 GemType이 포함되도록 기본 초기화 (PlayGameService.gameStart 참고)
            for (GemType gem : GemType.values()) {
                tokens.put(gem, 0);
            }
        }

        Map<GemType, Integer> bonuses = new HashMap<>();
        bonuses.put(DIAMOND, cardCount);

        // PlayerStateDto(player, score, tokens, bonuses, purchasedCardCount, nobleCount, turnOrder)
        return new PlayerStateDto(player, score, tokens, bonuses, cardCount, nobleCount, turnOrder);
    }

    // ⭐️ GameStateDto 생성자 순서 수정 (service 코드와 일치)
    // DTO 순서: boardState, playerStateDto, gameId, currentPlayer, isFinalRound, startingPlayerId, isGameOver, winner
    private GameStateDto createGameState(GamePlayerDto currentPlayer, boolean isFinalRoundFlag, String startingPlayerIdValue) {
        BoardStateDto boardState = new BoardStateDto(List.of(), List.of(), new HashMap<>(Map.of(
                DIAMOND, 4, SAPPHIRE, 4, EMERALD, 4, RUBY, 4, ONYX, 4, GOLD, 5
        )));

        return new GameStateDto(
                boardState,
                this.threePlayerStates,
                TEST_ROOM_ID,
                currentPlayer,
                false,
                null,
                isFinalRoundFlag,
                startingPlayerIdValue
        );
    }


    @BeforeEach
    void setUp() {
        // Mocking 초기 상태: 3인 게임
        PlayerStateDto p1State = createPlayerState(P1_ID, "Host", 0, 0, 0, 0, 0); // Turn 0, 선
        PlayerStateDto p2State = createPlayerState(P2_ID, "GuestA", 0, 0, 0, 0, 1); // Turn 1
        PlayerStateDto p3State = createPlayerState(P3_ID, "GuestB", 0, 0, 0, 0, 2); // Turn 2

        this.threePlayerStates = new ArrayList<>(List.of(p1State, p2State, p3State));

        // 초기 게임 상태: P1 턴, FinalRound 아님
        this.initialGameState = createGameState(p1State.getPlayer(), false, P1_ID);

        // 기본적으로 아무 행동도 하지 않음을 가정
        when(selectTokenStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.empty());
        when(cardSelectionStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.empty());

        // ❌ Mockito UnnecessaryStubbingException 방지를 위해 save stubbing을 제거하고 필요한 테스트에만 추가합니다.
    }

    // =================================================================
    // 1. 게임 진행 테스트 (Game Continuation)
    // =================================================================

    @Test
    @DisplayName("성공: 15점 미만일 때 endTurn 호출 시 isFinalRound가 false이고 턴이 다음 플레이어로 넘어가야 한다.")
    void endTurn_ShouldAdvanceTurn_WhenNoOneReachesFifteen() {
        // Given: P1 점수 14점 (15점 미만)
        this.threePlayerStates.get(0).setScore(14);

        when(gameStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(initialGameState));
        // ⭐️ 게임이 계속되므로 save를 스터빙
        when(gameStateRepository.save(any(GameStateDto.class))).thenAnswer(i -> i.getArgument(0));


        // When
        GameStateDto result = playGameService.endTurn(TEST_ROOM_ID);

        // Then
        assertThat(result.isFinalRound()).isFalse();
        assertThat(result.getCurrentPlayer().getPlayerId()).isEqualTo(P2_ID);
        assertThat(result.isGameOver()).isFalse();
        verify(gameStateRepository, times(1)).save(any(GameStateDto.class));
        verify(gameStateRepository, times(0)).deleteById(any());
    }

    // =================================================================
    // 2. 최종 라운드 시작 테스트 (Final Round Start)
    // =================================================================

    @Test
    @DisplayName("성공: P1이 15점을 달성하고 턴을 종료하면 isFinalRound가 true가 되고 턴이 P2로 넘어가야 한다.")
    void endTurn_ShouldStartFinalRound_WhenPlayerReachesFifteen() {
        // Given: P1 점수 15점 달성
        this.threePlayerStates.get(0).setScore(15);

        when(gameStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(initialGameState));
        // ⭐️ 게임이 계속되므로 save를 스터빙
        when(gameStateRepository.save(any(GameStateDto.class))).thenAnswer(i -> i.getArgument(0));

        // When: P1 턴 종료
        GameStateDto result = playGameService.endTurn(TEST_ROOM_ID);

        // Then
        assertThat(result.isFinalRound()).isTrue(); // Final Round 시작
        assertThat(result.getCurrentPlayer().getPlayerId()).isEqualTo(P2_ID); // 턴은 정상적으로 P2로 넘어가야 함
        assertThat(result.isGameOver()).isFalse();
        verify(gameStateRepository, times(1)).save(any(GameStateDto.class));
        verify(gameStateRepository, times(0)).deleteById(any());
    }

    // =================================================================
    // 3. 최종 라운드 완료 테스트 (Game Over)
    // =================================================================

    @Test
    @DisplayName("성공: 최종 라운드가 시작된 후 선 플레이어(P1)에게 턴이 다시 돌아오면 게임이 종료되어야 한다.")
    void endTurn_ShouldEndGame_WhenTurnReturnsToStartingPlayer() {
        // Given: P3 턴이고, 최종 라운드가 이미 시작된 상태

        // P1이 15점을 달성했다고 가정
        this.threePlayerStates.stream().filter(p -> p.getPlayer().getPlayerId().equals(P1_ID)).findFirst().get().setScore(15);

        // P3 턴 상태를 재현
        GameStateDto finalRoundState = createGameState(
                this.threePlayerStates.get(2).getPlayer(), // P3이 현재 턴 (인덱스 2)
                true, // isFinalRound
                P1_ID // 선 플레이어 P1 (인덱스 0)
        );

        when(gameStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(finalRoundState));

        // When: P3 턴 종료 -> advanceTurn 로직이 P1을 다음 턴으로 설정 -> isCurrentPlayerStartingPlayer가 true 반환 -> Game Over
        GameStateDto result = playGameService.endTurn(TEST_ROOM_ID);

        // Then
        assertThat(result.isFinalRound()).isTrue();
        assertThat(result.isGameOver()).isTrue(); // Game Over 상태 확인
        assertThat(result.getWinner()).isNotNull(); // 승자 확인
        verify(gameStateRepository, times(0)).save(any(GameStateDto.class)); // ⭐️ save 호출되면 안 됨
        verify(gameStateRepository, times(1)).deleteById(TEST_ROOM_ID);
    }

    // =================================================================
    // 4. 승자 결정 테스트 (Winner Determination Logic)
    // =================================================================

    // 헬퍼: GameStateDto에 플레이어 상태를 강제로 설정
    private void setPlayerStates(GameStateDto gameState, List<PlayerStateDto> states) {
        gameState.getPlayerStateDto().clear();
        gameState.getPlayerStateDto().addAll(states);
    }

    @Test
    @DisplayName("승자 결정: 1순위 - 점수가 가장 높은 플레이어 승리")
    void determineWinner_ShouldSelectHighestScore() {
        // Given: P2가 점수만 높음 (17점 vs 16점)
        PlayerStateDto p1 = createPlayerState(P1_ID, "P1", 16, 5, 1, 10, 0);
        PlayerStateDto p2 = createPlayerState(P2_ID, "P2", 17, 10, 0, 0, 1);

        setPlayerStates(initialGameState, List.of(p1, p2));
        initialGameState.setFinalRound(true);
        initialGameState.setCurrentPlayer(p2.getPlayer()); // P2 턴 종료 -> P1 턴 (게임 종료)

        when(gameStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(initialGameState));

        // When
        GameStateDto result = playGameService.endTurn(TEST_ROOM_ID);

        // Then
        assertThat(result.isGameOver()).isTrue();
        assertThat(result.getWinner().getPlayerId()).isEqualTo(P2_ID);
        verify(gameStateRepository, times(0)).save(any(GameStateDto.class));
    }

    @Test
    @DisplayName("승자 결정: 2순위 - 동점 시 개발 카드 수가 가장 적은 플레이어 승리")
    void determineWinner_ShouldSelectFewerCards() {
        // Given: 점수 동일(16점), P2가 카드가 적음 (규칙 6-1)
        PlayerStateDto p1 = createPlayerState(P1_ID, "P1", 16, 5, 1, 10, 0);
        PlayerStateDto p2 = createPlayerState(P2_ID, "P2", 16, 4, 1, 10, 1);

        setPlayerStates(initialGameState, List.of(p1, p2));
        initialGameState.setFinalRound(true);
        initialGameState.setCurrentPlayer(p2.getPlayer());

        when(gameStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(initialGameState));

        // When
        GameStateDto result = playGameService.endTurn(TEST_ROOM_ID);

        // Then
        assertThat(result.isGameOver()).isTrue();
        assertThat(result.getWinner().getPlayerId()).isEqualTo(P2_ID);
        verify(gameStateRepository, times(0)).save(any(GameStateDto.class));
    }

    @Test
    @DisplayName("승자 결정: 3순위 - 동점 시 귀족 타일 수가 가장 많은 플레이어 승리")
    void determineWinner_ShouldSelectMoreNobles() {
        // Given: 점수/카드 수 동일, P2가 귀족 타일이 많음 (규칙 6-2)
        PlayerStateDto p1 = createPlayerState(P1_ID, "P1", 16, 5, 1, 10, 0);
        PlayerStateDto p2 = createPlayerState(P2_ID, "P2", 16, 5, 2, 10, 1);

        setPlayerStates(initialGameState, List.of(p1, p2));
        initialGameState.setFinalRound(true);
        initialGameState.setCurrentPlayer(p2.getPlayer());

        when(gameStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(initialGameState));

        // When
        GameStateDto result = playGameService.endTurn(TEST_ROOM_ID);

        // Then
        assertThat(result.isGameOver()).isTrue();
        assertThat(result.getWinner().getPlayerId()).isEqualTo(P2_ID);
        verify(gameStateRepository, times(0)).save(any(GameStateDto.class));
    }

    @Test
    @DisplayName("승자 결정: 4순위 - 동점 시 남은 보석 토큰 수가 많은 플레이어 승리")
    void determineWinner_ShouldSelectMoreTokens() {
        // Given: 점수/카드 수/귀족 타일 수 동일, P1이 토큰이 많음 (규칙 6-3)
        PlayerStateDto p1 = createPlayerState(P1_ID, "P1", 16, 5, 1, 10, 0); // 토큰 10개
        PlayerStateDto p2 = createPlayerState(P2_ID, "P2", 16, 5, 1, 9, 1); // 토큰 9개

        setPlayerStates(initialGameState, List.of(p1, p2));
        initialGameState.setFinalRound(true);
        initialGameState.setCurrentPlayer(p2.getPlayer());

        when(gameStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(initialGameState));

        // When
        GameStateDto result = playGameService.endTurn(TEST_ROOM_ID);

        // Then
        assertThat(result.isGameOver()).isTrue();
        assertThat(result.getWinner().getPlayerId()).isEqualTo(P1_ID); // P1 (10개) 승리
        verify(gameStateRepository, times(0)).save(any(GameStateDto.class));
    }

    @Test
    @DisplayName("승자 결정: 최종 순위 - 모든 조건 동일 시 후공 플레이어 승리")
    void determineWinner_ShouldSelectLaterTurnOrder() {
        // Given: 모든 조건 동일 (규칙 6-4)
        PlayerStateDto p1 = createPlayerState(P1_ID, "P1", 16, 5, 1, 10, 0); // 선공 (TurnOrder 0)
        PlayerStateDto p2 = createPlayerState(P2_ID, "P2", 16, 5, 1, 10, 1); // 후공 (TurnOrder 1)

        setPlayerStates(initialGameState, List.of(p1, p2));
        initialGameState.setFinalRound(true);
        initialGameState.setCurrentPlayer(p2.getPlayer());

        when(gameStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(initialGameState));

        // When
        GameStateDto result = playGameService.endTurn(TEST_ROOM_ID);

        // Then
        assertThat(result.isGameOver()).isTrue();
        assertThat(result.getWinner().getPlayerId()).isEqualTo(P2_ID); // P2 (후공) 승리
        verify(gameStateRepository, times(0)).save(any(GameStateDto.class));
    }
}