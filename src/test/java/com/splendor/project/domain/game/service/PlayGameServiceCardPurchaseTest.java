package com.splendor.project.domain.game.service;

import com.splendor.project.domain.data.GemType;
import com.splendor.project.domain.data.StaticCard;
import com.splendor.project.domain.game.dto.request.SelectCardRequestDto;
import com.splendor.project.domain.game.dto.response.BoardStateDto;
import com.splendor.project.domain.game.dto.response.GamePlayerDto;
import com.splendor.project.domain.game.dto.response.GameStateDto;
import com.splendor.project.domain.game.dto.response.PlayerStateDto;
import com.splendor.project.domain.game.dto.response.SelectionCardStateDto;
import com.splendor.project.domain.game.logic.PlayerStateCalculator;
import com.splendor.project.domain.game.repository.GameStateRepository;
import com.splendor.project.domain.game.repository.SelectTokenStateRepository;
import com.splendor.project.domain.game.repository.SelectionCardStateRepository;
import com.splendor.project.exception.ErrorCode;
import com.splendor.project.exception.GameLogicException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.splendor.project.domain.data.GemType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayGameServiceCommitTest {

    @Mock private GameStateRepository gameStateRepository;
    @Mock private SelectTokenStateRepository selectTokenStateRepository;
    @Mock private SelectionCardStateRepository cardSelectionStateRepository;
    @Mock private GameStaticDataLoader staticDataLoader;

    @InjectMocks private PlayGameService playGameService;

    private final Long TEST_ROOM_ID = 1L;
    private final String HOST_ID = "host-id-1";
    private final String GUEST_ID = "guest-id-2";
    private final int CARD_ID_SIMPLE = 10;
    private final int CARD_ID_EXPENSIVE = 50;

    private GamePlayerDto hostGamePlayer;
    private GamePlayerDto guestGamePlayer;
    private GameStateDto initialGameState;
    private PlayerStateDto hostInitialState;
    private List<StaticCard> allStaticCards;


    @BeforeEach
    void setUp() throws Exception {
        hostGamePlayer = new GamePlayerDto("HostName", HOST_ID);
        guestGamePlayer = new GamePlayerDto("GuestName", GUEST_ID);

        StaticCard simpleCard = new StaticCard(CARD_ID_SIMPLE, 1, 0, DIAMOND, 0, 1, 1, 1, 2);
        StaticCard expensiveCard = new StaticCard(CARD_ID_EXPENSIVE, 2, 2, SAPPHIRE, 0, 3, 0, 5, 0);
        allStaticCards = List.of(simpleCard, expensiveCard);

        Map<GemType, Integer> hostTokens = new HashMap<>(Map.of(
                DIAMOND, 1, SAPPHIRE, 2, EMERALD, 2, RUBY, 2, ONYX, 3, GOLD, 1));
        Map<GemType, Integer> hostBonuses = new HashMap<>(Map.of(DIAMOND, 0, SAPPHIRE, 0, EMERALD, 0, RUBY, 0, ONYX, 0, GOLD, 0));

        // 1. PlayerStateDto 생성자 수정 (purchasedCardCount, nobleCount, turnOrder 추가)
        hostInitialState = new PlayerStateDto(hostGamePlayer, 0, hostTokens, hostBonuses, 0, 0, 0);

        List<PlayerStateDto> playerStates = List.of(
                hostInitialState,
                // Guest 플레이어 초기화 시에도 0, 0, 1 추가
                new PlayerStateDto(guestGamePlayer, 0, new HashMap<>(), new HashMap<>(), 0, 0, 1)
        );

        Map<GemType, Integer> boardTokens = new HashMap<>(Map.of(
                DIAMOND, 4, SAPPHIRE, 4, EMERALD, 4, RUBY, 4, ONYX, 4, GOLD, 5));

        BoardStateDto boardState = new BoardStateDto(
                new ArrayList<>(List.of(new ArrayList<>(List.of(simpleCard)), new ArrayList<>(), new ArrayList<>())),
                Collections.emptyList(),
                boardTokens
        );

        // 2. GameStateDto 생성자 수정 (isFinalRound, startingPlayerId, isGameOver, winner 추가)
        initialGameState = new GameStateDto(
                boardState,
                playerStates,
                TEST_ROOM_ID,
                hostGamePlayer,
                false, // isGameOver
                null, // winner
                false, // isFinalRound
                HOST_ID // startingPlayerId
        );

        when(gameStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(initialGameState));
    }

    // =================================================================
    // A. 성공 케이스 (endTurn 커밋 검증)
    // =================================================================

    @Test
    @DisplayName("성공: 카드를 선택한 후 endTurn을 누르면 구매가 확정되고 상태가 업데이트되어야 한다.")
    void endTurn_ShouldCommitCardPurchase_WhenCardIsSelected() {
        // 1. save 스터빙을 성공 테스트 내부로 이동
        when(gameStateRepository.save(any(GameStateDto.class))).thenAnswer(i -> i.getArgument(0));
        when(staticDataLoader.getAllCards()).thenReturn(allStaticCards);

        // Given
        StaticCard cardToBuy = allStaticCards.stream().filter(c -> c.id() == CARD_ID_SIMPLE).findFirst().orElseThrow();

        // 중간 상태 Mock: 카드가 선택된 상태
        SelectionCardStateDto selectedState = new SelectionCardStateDto(TEST_ROOM_ID, HOST_ID);
        selectedState.setCardIdToBuy(CARD_ID_SIMPLE);

        // endTurn이 호출되었을 때 이 상태를 찾도록 Mock 설정
        when(cardSelectionStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(selectedState));
        when(selectTokenStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.empty()); // 토큰 획득 시도 없음

        // Mock 지불 금액: {S: 1, E: 1, R: 1, O: 2}
        Map<GemType, Integer> expectedPayment = Map.of(SAPPHIRE, 1, EMERALD, 1, RUBY, 1, ONYX, 2);

        try (MockedStatic<PlayerStateCalculator> mockedCalculator = mockStatic(PlayerStateCalculator.class)) {
            mockedCalculator.when(() -> PlayerStateCalculator.calculatePayment(any(), eq(cardToBuy)))
                    .thenReturn(expectedPayment);

            // When: 턴 종료 요청
            GameStateDto result = playGameService.endTurn(TEST_ROOM_ID);

            // Then
            PlayerStateDto hostResultState = result.getPlayerStateDto().get(0);
            Map<GemType, Integer> boardResultTokens = result.getBoardStateDto().getAvailableTokens();
            List<StaticCard> level1Cards = result.getBoardStateDto().getCards().get(0);

            // 1. 상태 변화 검증
            assertThat(hostResultState.getBonuses().get(DIAMOND)).isEqualTo(1); // 보너스 획득
            assertThat(hostResultState.getTokens().get(SAPPHIRE)).isEqualTo(1);
            assertThat(boardResultTokens.get(SAPPHIRE)).isEqualTo(5); // 토큰 환수

            // 2. 턴 변경 검증
            assertThat(result.getCurrentPlayer().getPlayerId()).isEqualTo(GUEST_ID);

            // 3. Cleanup 검증
            verify(cardSelectionStateRepository, times(1)).deleteById(TEST_ROOM_ID);
            verify(gameStateRepository, times(1)).save(any(GameStateDto.class));
        }
    }


    // =================================================================
    // B. 실패 케이스 (commit 직전 검증 실패)
    // =================================================================

    @Test
    @DisplayName("실패: 선택된 카드가 토큰 부족으로 구매 불가 상태일 때 endTurn을 누르면 NOT_ENOUGH_TOKENS 예외가 발생해야 한다.")
    void endTurn_ShouldThrowException_WhenTokensAreNotEnoughAtCommit() {
        // Given
        StaticCard cardToBuy = allStaticCards.stream().filter(c -> c.id() == CARD_ID_EXPENSIVE).findFirst().orElseThrow();

        // 중간 상태 Mock: 고가 카드가 선택된 상태
        SelectionCardStateDto selectedState = new SelectionCardStateDto(TEST_ROOM_ID, HOST_ID);
        selectedState.setCardIdToBuy(CARD_ID_EXPENSIVE);

        when(cardSelectionStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(selectedState));
        when(selectTokenStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.empty());
        when(staticDataLoader.getAllCards()).thenReturn(allStaticCards);

        // Mock 지불 계산기가 부족 예외를 던지도록 설정
        try (MockedStatic<PlayerStateCalculator> mockedCalculator = mockStatic(PlayerStateCalculator.class)) {
            mockedCalculator.when(() -> PlayerStateCalculator.calculatePayment(any(), eq(cardToBuy)))
                    .thenThrow(new GameLogicException(ErrorCode.NOT_ENOUGH_TOKENS));

            // When & Then
            GameLogicException exception = assertThrows(GameLogicException.class, () -> {
                playGameService.endTurn(TEST_ROOM_ID);
            });

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_ENOUGH_TOKENS);

            // 턴이 넘어가지 않고, 상태 저장/삭제도 되지 않아야 함 (Transactional 롤백)
            verify(cardSelectionStateRepository, times(0)).deleteById(any());
            verify(gameStateRepository, times(0)).save(any());
        }
    }

    // =================================================================
    // C. 선택/취소 (selectCard) 실패 케이스
    // =================================================================

    @Test
    @DisplayName("실패: 다른 카드를 선택한 상태에서 새 카드를 선택하면 ANOTHER_CARD_ALREADY_SELECTED 예외가 발생해야 한다.")
    void selectCard_ShouldFail_WhenAnotherCardIsAlreadySelected() {
        // Given
        SelectionCardStateDto selectedState = new SelectionCardStateDto(TEST_ROOM_ID, HOST_ID);
        selectedState.setCardIdToBuy(CARD_ID_SIMPLE); // 이미 CARD_ID_SIMPLE 선택됨

        SelectCardRequestDto request = new SelectCardRequestDto(TEST_ROOM_ID, HOST_ID, CARD_ID_EXPENSIVE, true); // 다른 카드 선택 시도

        when(cardSelectionStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(selectedState));
        when(selectTokenStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.empty());
        when(gameStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(initialGameState));
        when(staticDataLoader.getAllCards()).thenReturn(allStaticCards);

        // When & Then
        GameLogicException exception = assertThrows(GameLogicException.class, () -> {
            playGameService.selectCard(request);
        });

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ANOTHER_CARD_ALREADY_SELECTED);
        verify(cardSelectionStateRepository, times(0)).save(any()); // 상태 저장되면 안 됨
    }
}