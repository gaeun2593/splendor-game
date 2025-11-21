package com.splendor.project.domain.game.service;

import com.splendor.project.domain.data.GemType;
import com.splendor.project.domain.game.dto.request.DiscardTokenRequestDto;
import com.splendor.project.domain.game.dto.request.SelectTokenRequestDto;
import com.splendor.project.domain.game.dto.response.BoardStateDto;
import com.splendor.project.domain.game.dto.response.GamePlayerDto;
import com.splendor.project.domain.game.dto.response.GameStateDto;
import com.splendor.project.domain.game.dto.response.PlayerStateDto;
import com.splendor.project.domain.game.dto.response.SelectTokenStateDto;
import com.splendor.project.domain.game.repository.GameStateRepository;
import com.splendor.project.domain.game.repository.SelectTokenStateRepository;
import com.splendor.project.domain.player.entity.Player;
import com.splendor.project.domain.room.entity.Room;
import com.splendor.project.domain.room.entity.RoomStatus;
import com.splendor.project.domain.room.repository.RoomRepository;
import com.splendor.project.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
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
class PlayGameServiceTest {

    @Mock
    private InitialGameService initialGameService;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private GameStateRepository gameStateRepository;

    @Mock
    private SelectTokenStateRepository selectTokenStateRepository;

    @Mock // ✨ 새 Validator Mock 추가
    private TokenAcquisitionValidator tokenAcquisitionValidator;

    @InjectMocks // ✨ Mock 객체들을 PlayGameService에 주입
    private PlayGameService playGameService;

    private final Long TEST_ROOM_ID = 1L;
    private final String HOST_ID = "host-id";
    private final String GUEST_ID = "guest-id";
    private Room testRoom;
    private Player hostPlayer;
    private Player guestPlayer;
    private BoardStateDto mockBoardState;
    private GameStateDto initialGameState;
    private GamePlayerDto hostGamePlayer;
    private GamePlayerDto guestGamePlayer;

    @BeforeEach
    void setUp() throws Exception {
        testRoom = new Room("Test Room", RoomStatus.WAITING);
        setRoomId(testRoom, TEST_ROOM_ID);

        hostPlayer = new Player(testRoom, "HostName", true, true);
        setPlayerId(hostPlayer, HOST_ID);

        guestPlayer = new Player(testRoom, "GuestName", true, true);
        setPlayerId(guestPlayer, GUEST_ID);

        hostGamePlayer = new GamePlayerDto("HostName", HOST_ID);
        guestGamePlayer = new GamePlayerDto("GuestName", GUEST_ID);

        // 1단계 테스트를 위한 MockBoardState
        mockBoardState = new BoardStateDto(
                List.of(), List.of(), Map.of(DIAMOND, 4, SAPPHIRE, 4, RUBY, 4, EMERALD, 4, ONYX, 4, GOLD, 5)
        );

        // 2단계 테스트를 위한 초기 게임 상태 DTO 생성
        List<PlayerStateDto> playerStates = List.of(
                new PlayerStateDto(hostGamePlayer, 0,
                        new HashMap<>(Map.of(DIAMOND, 0, RUBY, 0, EMERALD, 0, SAPPHIRE, 0, ONYX, 0, GOLD, 0)),
                        Map.of(DIAMOND, 0, RUBY, 0, EMERALD, 0, SAPPHIRE, 0, ONYX, 0, GOLD, 0)),
                new PlayerStateDto(guestGamePlayer, 0,
                        new HashMap<>(Map.of(DIAMOND, 0, RUBY, 0, EMERALD, 0, SAPPHIRE, 0, ONYX, 0, GOLD, 0)),
                        Map.of(DIAMOND, 0, RUBY, 0, EMERALD, 0, SAPPHIRE, 0, ONYX, 0, GOLD, 0))
        );
        initialGameState = new GameStateDto(
                new BoardStateDto(List.of(), List.of(), new HashMap<>(Map.of(
                        DIAMOND, 4, SAPPHIRE, 4, EMERALD, 4, RUBY, 4, ONYX, 4, GOLD, 5
                ))),
                playerStates,
                TEST_ROOM_ID,
                hostGamePlayer
        );
    }

    // Room ID 설정을 위한 리플렉션 헬퍼 메서드
    private void setRoomId(Room room, Long id) throws Exception {
        Field field = Room.class.getDeclaredField("roomId");
        field.setAccessible(true);
        field.set(room, id);
    }

    // Player ID 설정을 위한 리플렉션 헬퍼 메서드
    private void setPlayerId(Player player, String id) throws Exception {
        Field field = Player.class.getDeclaredField("playerId");
        field.setAccessible(true);
        field.set(player, id);
    }

    // =================================================================
    // 1. 게임 시작 테스트
    // =================================================================

    @Test
    @DisplayName("게임 시작 시 GameStateDto가 생성되고 Redis에 저장되어야 한다.")
    void gameStart_ShouldSaveGameStateToRedis() {
        // Given
        when(roomRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(testRoom));
        when(initialGameService.initializeGame()).thenReturn(mockBoardState);
        when(gameStateRepository.save(any(GameStateDto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        GameStateDto resultDto = playGameService.gameStart(TEST_ROOM_ID);

        // Then
        assertThat(resultDto.getGameId()).isEqualTo(TEST_ROOM_ID);
        verify(gameStateRepository, times(1)).save(any(GameStateDto.class));
    }

    // =================================================================
    // 2. 토큰 선택 (selectToken) 테스트 (Validator Mocking 적용)
    // =================================================================

    @Test
    @DisplayName("성공: 다른 보석 3개를 선택하고 임시 상태에 저장되어야 한다.")
    void selectToken_ShouldStoreThreeDifferentTokens() {
        // Given
        SelectTokenRequestDto request1 = new SelectTokenRequestDto(TEST_ROOM_ID, HOST_ID, DIAMOND, true);
        SelectTokenRequestDto request2 = new SelectTokenRequestDto(TEST_ROOM_ID, HOST_ID, SAPPHIRE, true);
        SelectTokenRequestDto request3 = new SelectTokenRequestDto(TEST_ROOM_ID, HOST_ID, RUBY, true);

        // Mocking 상태 유지를 위한 mutable DTO
        SelectTokenStateDto mutableSelectState = new SelectTokenStateDto(TEST_ROOM_ID, HOST_ID);

        when(gameStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(initialGameState));

        // findById가 항상 mutableSelectState를 반환하도록 설정하여 상태 누적 재현
        when(selectTokenStateRepository.findById(TEST_ROOM_ID))
                .thenReturn(Optional.of(mutableSelectState));

        when(selectTokenStateRepository.save(any(SelectTokenStateDto.class))).thenReturn(mutableSelectState);

        // Validator는 성공(Do Nothing)이 기본 동작이므로 별도 설정 불필요

        // When
        playGameService.selectToken(request1);
        playGameService.selectToken(request2);
        Map<GemType, Integer> result = playGameService.selectToken(request3);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result)
                .containsEntry(DIAMOND, 1)
                .containsEntry(SAPPHIRE, 1)
                .containsEntry(RUBY, 1);
        verify(selectTokenStateRepository, times(3)).save(any(SelectTokenStateDto.class));
        // Validator가 3번 호출되었는지 검증
        verify(tokenAcquisitionValidator, times(3)).validatePartialTokenAcquisition(any(), any());
    }

    @Test
    @DisplayName("실패: 이미 3개를 선택했는데 4번째 토큰을 선택하면 예외가 발생해야 한다.")
    void selectToken_ShouldFail_WhenSelectingFourthToken() {
        // Given
        SelectTokenRequestDto request = new SelectTokenRequestDto(TEST_ROOM_ID, HOST_ID, ONYX, true);

        SelectTokenStateDto selectState = new SelectTokenStateDto(TEST_ROOM_ID, HOST_ID);
        selectState.getTokensToTake().putAll(Map.of(DIAMOND, 1, SAPPHIRE, 1, RUBY, 1)); // 총 3개

        when(selectTokenStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(selectState));
        when(gameStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(initialGameState));

        // ✨ Validator Mocking: 4번째 토큰 선택 시 발생하는 예외를 던지도록 설정
        doThrow(new IllegalArgumentException(ErrorCode.INVALID_TOKEN_ACTION.getMessage() + " (최대 3개 초과)"))
                .when(tokenAcquisitionValidator).validatePartialTokenAcquisition(any(), any());


        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            playGameService.selectToken(request);
        }, ErrorCode.INVALID_TOKEN_ACTION.getMessage() + " (최대 3개 초과)");

        verify(selectTokenStateRepository, times(0)).save(any());
        verify(tokenAcquisitionValidator, times(1)).validatePartialTokenAcquisition(any(), any());
    }

    @Test
    @DisplayName("실패: 보드에 4개 미만인 토큰을 2개 선택하면 예외가 발생해야 한다.")
    void selectToken_ShouldFail_WhenTakingTwoSameTokensAndLessThanFourAvailable() {
        // Given
        // 테스트를 위해 초기 상태에서 ONYX 토큰 개수를 3개로 설정 (4개 미만)
        initialGameState.getBoardStateDto().getAvailableTokens().put(ONYX, 3);

        SelectTokenRequestDto request1 = new SelectTokenRequestDto(TEST_ROOM_ID, HOST_ID, ONYX, true);
        SelectTokenRequestDto request2 = new SelectTokenRequestDto(TEST_ROOM_ID, HOST_ID, ONYX, true);

        when(gameStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(initialGameState));

        SelectTokenStateDto mutableSelectState = new SelectTokenStateDto(TEST_ROOM_ID, HOST_ID);

        // Mocking 상태 유지를 위한 설정
        when(selectTokenStateRepository.findById(TEST_ROOM_ID))
                .thenReturn(Optional.of(mutableSelectState));

        when(selectTokenStateRepository.save(any(SelectTokenStateDto.class))).thenReturn(mutableSelectState);

        // ✨ Validator Mocking: 첫 번째 호출은 성공, 두 번째 호출은 실패하도록 설정
        doNothing() // 1. 첫 번째 호출은 성공
                .doThrow(new IllegalArgumentException(ErrorCode.INVALID_TWO_TOKEN_RULE.getMessage())) // 2. 두 번째 호출은 실패
                .when(tokenAcquisitionValidator).validatePartialTokenAcquisition(any(), any());


        // 1. 첫 번째 선택 (ONYX: 1) -> 성공
        playGameService.selectToken(request1);

        // 2. 두 번째 선택 (ONYX: 2 시도 -> 실패 예상)
        assertThrows(IllegalArgumentException.class, () -> {
            playGameService.selectToken(request2);
        }, ErrorCode.INVALID_TWO_TOKEN_RULE.getMessage());

        // save가 1번만 호출되었는지 검증 (첫 번째 성공만)
        verify(selectTokenStateRepository, times(1)).save(any());
        // Validator가 총 2번 호출되었는지 검증
        verify(tokenAcquisitionValidator, times(2)).validatePartialTokenAcquisition(any(), any());
    }

    // =================================================================
    // 2.5. 보안 취약점 방지 테스트 (새로운 턴 검증 로직 검증)
    // =================================================================

    @Test
    @DisplayName("실패: 현재 턴이 아닌 유저가 토큰 선택을 시도하면 IllegalStateException이 발생해야 한다.")
    void selectToken_ShouldFail_WhenNotCurrentPlayerTriesToSelect() {
        // Given
        // 현재 턴은 HOST_ID (setUp에서 설정)
        // GUEST_ID가 선택을 시도하는 요청
        SelectTokenRequestDto request = new SelectTokenRequestDto(TEST_ROOM_ID, GUEST_ID, DIAMOND, true);

        when(gameStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(initialGameState));

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            playGameService.selectToken(request);
        }, "현재 턴이 아닙니다. 토큰을 선택할 수 없습니다.");

        // save는 물론, Validator도 호출되지 않았는지 확인
        verify(selectTokenStateRepository, times(0)).save(any());
        verify(tokenAcquisitionValidator, times(0)).validatePartialTokenAcquisition(any(), any());
    }


    // =================================================================
    // 3. 토큰 버리기 (discardToken) 테스트
    // =================================================================

    @Test
    @DisplayName("성공: 플레이어 토큰을 버리면 보드 토큰이 증가하고 플레이어 토큰이 감소해야 한다.")
    void discardToken_ShouldMoveTokenFromPlayerToBoard() {
        // Given
        // 호스트의 현재 토큰을 11개로 설정 (버리기 모드 가정)
        PlayerStateDto hostState = initialGameState.getPlayerStateDto().stream()
                .filter(p -> p.getPlayer().getPlayerId().equals(HOST_ID))
                .findFirst().orElseThrow();
        hostState.getTokens().putAll(new HashMap<>(Map.of(DIAMOND, 5, SAPPHIRE, 3, RUBY, 3))); // 총 11개

        // 보드 토큰 초기 상태: DIAMOND 4개
        initialGameState.getBoardStateDto().getAvailableTokens().put(DIAMOND, 4);

        when(gameStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(initialGameState));
        when(gameStateRepository.save(any(GameStateDto.class))).thenAnswer(i -> i.getArgument(0));

        DiscardTokenRequestDto request = new DiscardTokenRequestDto(TEST_ROOM_ID, HOST_ID, DIAMOND);

        // When
        GameStateDto result = playGameService.discardToken(request);

        // Then
        // 1. Redis save 호출 검증
        verify(gameStateRepository, times(1)).save(any(GameStateDto.class));

        // 2. 플레이어 토큰 검증: DIAMOND 5 -> 4
        PlayerStateDto hostResultState = result.getPlayerStateDto().stream()
                .filter(p -> p.getPlayer().getPlayerId().equals(HOST_ID))
                .findFirst().orElseThrow();
        assertThat(hostResultState.getTokens().get(DIAMOND)).isEqualTo(4);
        assertThat(hostResultState.getTokens().values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(10); // 총 10개

        // 3. 보드 토큰 검증: DIAMOND 4 -> 5
        Map<GemType, Integer> boardTokens = result.getBoardStateDto().getAvailableTokens();
        assertThat(boardTokens.get(DIAMOND)).isEqualTo(5);
    }

    @Test
    @DisplayName("실패: 현재 턴 플레이어가 아니면 토큰을 버릴 수 없다.")
    void discardToken_ShouldFail_WhenNotCurrentPlayer() {
        // Given
        // 현재 턴은 HOST_ID (setUp에서 설정)
        DiscardTokenRequestDto request = new DiscardTokenRequestDto(TEST_ROOM_ID, GUEST_ID, DIAMOND); // GUEST가 시도

        when(gameStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(initialGameState));

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            playGameService.discardToken(request);
        }, "현재 턴이 아닙니다. 토큰을 버릴 수 없습니다.");
    }

    // =================================================================
    // 4. 턴 종료 (endTurn) 테스트
    // =================================================================

    @Test
    @DisplayName("성공: 선택된 토큰이 있으면 획득을 커밋하고 다음 턴으로 넘어가야 한다.")
    void endTurn_ShouldCommitAcquisitionAndAdvanceTurn() {
        // Given
        // 유효한 획득: DIAMOND 1개, SAPPHIRE 1개, RUBY 1개 선택 (총 3개, 3종)
        SelectTokenStateDto selectState = new SelectTokenStateDto(TEST_ROOM_ID, HOST_ID);
        selectState.getTokensToTake().putAll(Map.of(DIAMOND, 1, SAPPHIRE, 1, RUBY, 1));

        // Mock Redis 호출
        when(selectTokenStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(selectState));
        when(gameStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(initialGameState));
        when(gameStateRepository.save(any(GameStateDto.class))).thenAnswer(i -> i.getArgument(0));

        // Validator Mocking: 최종 검증이 성공하도록 설정 (Do Nothing)
        // Verify the final validator was called once and succeeded
        verify(tokenAcquisitionValidator, times(0)).validateTokenAcquisition(any(), any());

        // When
        GameStateDto result = playGameService.endTurn(TEST_ROOM_ID);

        // Then
        // 1. 토큰 획득(Commit) 검증
        PlayerStateDto hostResultState = result.getPlayerStateDto().stream()
                .filter(p -> p.getPlayer().getPlayerId().equals(HOST_ID))
                .findFirst().orElseThrow();
        assertThat(hostResultState.getTokens().get(DIAMOND)).isEqualTo(1);
        assertThat(hostResultState.getTokens().get(SAPPHIRE)).isEqualTo(1);
        assertThat(hostResultState.getTokens().get(RUBY)).isEqualTo(1);

        // 2. 보드 토큰 감소 검증
        Map<GemType, Integer> boardTokens = result.getBoardStateDto().getAvailableTokens();
        assertThat(boardTokens.get(DIAMOND)).isEqualTo(3); // 4 -> 3
        assertThat(boardTokens.get(SAPPHIRE)).isEqualTo(3); // 4 -> 3
        assertThat(boardTokens.get(RUBY)).isEqualTo(3);     // 4 -> 3

        // 3. 임시 선택 상태 삭제 검증 (Cleanup)
        verify(selectTokenStateRepository, times(1)).deleteById(TEST_ROOM_ID);

        // 4. 턴 변경 검증 (HOST -> GUEST)
        assertThat(result.getCurrentPlayer().getPlayerId()).isEqualTo(GUEST_ID);
        verify(gameStateRepository, times(1)).save(any(GameStateDto.class));

        // 최종 Validator 호출 검증
        verify(tokenAcquisitionValidator, times(1)).validateTokenAcquisition(any(), any());
    }

    @Test
    @DisplayName("성공: 선택된 토큰이 없으면 획득을 스킵하고 다음 턴으로 넘어가야 한다.")
    void endTurn_ShouldSkipAcquisitionAndAdvanceTurn() {
        // Given
        // Mock 중간 선택 상태: Optional.empty() (선택 안 함)
        when(selectTokenStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.empty());
        when(gameStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(initialGameState));
        when(gameStateRepository.save(any(GameStateDto.class))).thenAnswer(i -> i.getArgument(0));

        // When
        GameStateDto result = playGameService.endTurn(TEST_ROOM_ID);

        // Then
        // 1. 토큰 획득(Commit) 스킵 검증 (보드 토큰 변화 없어야 함)
        Map<GemType, Integer> boardTokens = result.getBoardStateDto().getAvailableTokens();
        assertThat(boardTokens.get(DIAMOND)).isEqualTo(4);

        // 2. 임시 선택 상태 삭제 검증 (Cleanup)
        verify(selectTokenStateRepository, times(1)).deleteById(TEST_ROOM_ID); // deleteById는 호출됨 (정리 로직)

        // 3. 턴 변경 검증 (HOST -> GUEST)
        assertThat(result.getCurrentPlayer().getPlayerId()).isEqualTo(GUEST_ID);
        verify(gameStateRepository, times(1)).save(any(GameStateDto.class));

        // 획득을 스킵했으므로 최종 Validator는 호출되지 않아야 함
        verify(tokenAcquisitionValidator, times(0)).validateTokenAcquisition(any(), any());
    }
}