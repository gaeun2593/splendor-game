package com.splendor.project.domain.game.service;

import com.splendor.project.domain.data.GemType;
import com.splendor.project.domain.game.dto.request.TakeTokenRequestDto;
import com.splendor.project.domain.game.dto.response.BoardStateDto;
import com.splendor.project.domain.game.dto.response.GamePlayerDto;
import com.splendor.project.domain.game.dto.response.GameStateDto;
import com.splendor.project.domain.game.dto.response.PlayerStateDto;
import com.splendor.project.domain.game.repository.GameStateRepository;
import com.splendor.project.domain.player.entity.Player;
import com.splendor.project.domain.room.entity.Room;
import com.splendor.project.domain.room.entity.RoomStatus;
import com.splendor.project.domain.room.repository.RoomRepository;
import com.splendor.project.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayGameServiceTest {

    @InjectMocks
    private PlayGameService playGameService;

    @Mock
    private InitialGameService initialGameService;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private GameStateRepository gameStateRepository;

    private final Long TEST_ROOM_ID = 1L;
    private final String HOST_ID = "host-id";
    private Room testRoom;
    private Player hostPlayer;
    private Player guestPlayer;
    private BoardStateDto mockBoardState;
    private GameStateDto initialGameState;

    @BeforeEach
    void setUp() throws Exception {
        testRoom = new Room("Test Room", RoomStatus.WAITING);
        setRoomId(testRoom, TEST_ROOM_ID);

        // Player의 playerId 필드에도 수동으로 값 설정 (Player 엔티티에서 UUID 생성 로직을 무시)
        hostPlayer = new Player(testRoom, "HostName", true, true);
        setPlayerId(hostPlayer, HOST_ID);

        guestPlayer = new Player(testRoom, "GuestName", true, true);

        // 1단계 테스트를 위한 MockBoardState
        mockBoardState = new BoardStateDto(
                List.of(), List.of(), Map.of(DIAMOND, 4, SAPPHIRE, 4, RUBY, 4, EMERALD, 4, ONYX, 4, GOLD, 5)
        );

        // 2단계 테스트를 위한 초기 게임 상태 DTO 생성
        List<PlayerStateDto> playerStates = List.of(
                new PlayerStateDto(new GamePlayerDto("HostName", HOST_ID), 0,
                        new HashMap<>(Map.of(DIAMOND, 0, RUBY, 0, EMERALD, 0, SAPPHIRE, 0, ONYX, 0, GOLD, 0)),
                        Map.of(DIAMOND, 0, RUBY, 0, EMERALD, 0, SAPPHIRE, 0, ONYX, 0, GOLD, 0)),
                new PlayerStateDto(new GamePlayerDto("GuestName", "guest-id"), 0,
                        Map.of(DIAMOND, 0, RUBY, 0, EMERALD, 0, SAPPHIRE, 0, ONYX, 0, GOLD, 0),
                        Map.of(DIAMOND, 0, RUBY, 0, EMERALD, 0, SAPPHIRE, 0, ONYX, 0, GOLD, 0))
        );
        initialGameState = new GameStateDto(
                new BoardStateDto(List.of(), List.of(), new HashMap<>(Map.of(
                        DIAMOND, 4, SAPPHIRE, 4, EMERALD, 4, RUBY, 4, ONYX, 4, GOLD, 5
                ))),
                playerStates,
                TEST_ROOM_ID,
                new GamePlayerDto("HostName", HOST_ID)
        );
    }

    // Room ID 설정을 위한 리플렉션 헬퍼 메서드 (기존 1단계에서 사용)
    private void setRoomId(Room room, Long id) throws Exception {
        Field field = Room.class.getDeclaredField("roomId");
        field.setAccessible(true);
        field.set(room, id);
    }

    // Player ID 설정을 위한 리플렉션 헬퍼 메서드 (새로 추가)
    private void setPlayerId(Player player, String id) throws Exception {
        Field field = Player.class.getDeclaredField("playerId");
        field.setAccessible(true);
        field.set(player, id);
    }

    @Test
    @DisplayName("게임 시작 시 GameStateDto가 생성되고 Redis에 저장되어야 한다.")
    void gameStart_ShouldSaveGameStateToRedis() {
        // Given
        // 1. RoomRepository Mock: findById 호출 시 testRoom 반환 설정
        when(roomRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(testRoom));

        // 2. InitialGameService Mock: initializeGame 호출 시 mockBoardState 반환 설정
        when(initialGameService.initializeGame()).thenReturn(mockBoardState);

        // 3. GameStateRepository Mock: save 호출 시 전달받은 DTO를 그대로 반환하도록 설정 (실제 저장처럼)
        when(gameStateRepository.save(any(GameStateDto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        GameStateDto resultDto = playGameService.gameStart(TEST_ROOM_ID);

        // Then
        // 1. 반환된 DTO 검증 (gameId가 null이 아닌 1L인지 확인)
        assertThat(resultDto.getGameId()).isEqualTo(TEST_ROOM_ID);
        assertThat(resultDto.getPlayerStateDto()).hasSize(2);
        assertThat(resultDto.getBoardStateDto().getAvailableTokens()).isEqualTo(mockBoardState.getAvailableTokens());

        // 2. [핵심 검증] GameStateRepository.save()가 1번 호출되었는지 검증
        verify(gameStateRepository, times(1)).save(any(GameStateDto.class));

        // 3. [보강 검증] 저장된 GameStateDto의 내용이 정확한지 확인
        ArgumentCaptor<GameStateDto> argumentCaptor = ArgumentCaptor.forClass(GameStateDto.class);
        verify(gameStateRepository).save(argumentCaptor.capture());

        GameStateDto savedDto = argumentCaptor.getValue();

        // 저장된 DTO에 보드 토큰 정보가 올바르게 포함되어 있는지 확인
        assertThat(savedDto.getBoardStateDto().getAvailableTokens())
                .containsEntry(DIAMOND, 4)
                .containsEntry(GOLD, 5);

        // 저장된 DTO에 플레이어 초기 토큰 정보가 올바르게 0으로 설정되어 있는지 확인
        savedDto.getPlayerStateDto().forEach(playerState -> {
            assertThat(playerState.getTokens().values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(0);
        });
    }

    // =================================================================
    // Step 2: 토큰 획득 로직 (takeTokens) 테스트
    // =================================================================

    @Test
    @DisplayName("성공: 다른 보석 3개를 획득하고 상태가 올바르게 업데이트되어야 한다.")
    void takeTokens_ShouldSuccess_WhenTakingThreeDifferentTokens() {
        // Given
        // Redis에서 초기 상태 로드 가정
        when(gameStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(initialGameState));

        Map<GemType, Integer> tokensToTake = Map.of(DIAMOND, 1, SAPPHIRE, 1, RUBY, 1);
        TakeTokenRequestDto request = new TakeTokenRequestDto(TEST_ROOM_ID, HOST_ID, tokensToTake);

        // When
        GameStateDto result = playGameService.takeTokens(request);

        // Then
        // 1. Redis save 호출 검증
        verify(gameStateRepository, times(1)).save(any(GameStateDto.class));

        // 2. 보드 상태 업데이트 검증
        Map<GemType, Integer> boardTokens = result.getBoardStateDto().getAvailableTokens();
        assertThat(boardTokens.get(DIAMOND)).isEqualTo(3); // 4 - 1 = 3
        assertThat(boardTokens.get(SAPPHIRE)).isEqualTo(3); // 4 - 1 = 3
        assertThat(boardTokens.get(RUBY)).isEqualTo(3);     // 4 - 1 = 3
        assertThat(boardTokens.get(EMERALD)).isEqualTo(4); // 나머지 불변

        // 3. 플레이어 상태 업데이트 검증
        PlayerStateDto hostState = result.getPlayerStateDto().stream()
                .filter(p -> p.getPlayer().getPlayerId().equals(HOST_ID))
                .findFirst().orElseThrow();

        assertThat(hostState.getTokens().get(DIAMOND)).isEqualTo(1);
        assertThat(hostState.getTokens().get(SAPPHIRE)).isEqualTo(1);
        assertThat(hostState.getTokens().get(RUBY)).isEqualTo(1);
        assertThat(hostState.getTokens().values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(3);
    }


    @Test
    @DisplayName("성공: 같은 보석 2개를 가져갈 때, 해당 보석이 4개 이상 남아있으면 성공해야 한다. (사용자 요구 조건)")
    void takeTokens_ShouldSuccess_WhenTakingTwoSameTokensAndFourOrMoreAvailable() {
        // Given
        when(gameStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(initialGameState));

        Map<GemType, Integer> tokensToTake = Map.of(DIAMOND, 2);
        TakeTokenRequestDto request = new TakeTokenRequestDto(TEST_ROOM_ID, HOST_ID, tokensToTake); // DIAMOND는 초기 4개

        // When
        GameStateDto result = playGameService.takeTokens(request);

        // Then
        // 1. 보드 상태 업데이트 검증
        Map<GemType, Integer> boardTokens = result.getBoardStateDto().getAvailableTokens();
        assertThat(boardTokens.get(DIAMOND)).isEqualTo(2); // 4 - 2 = 2

        // 2. 플레이어 상태 업데이트 검증
        PlayerStateDto hostState = result.getPlayerStateDto().stream()
                .filter(p -> p.getPlayer().getPlayerId().equals(HOST_ID))
                .findFirst().orElseThrow();
        assertThat(hostState.getTokens().get(DIAMOND)).isEqualTo(2);
    }


    @Test
    @DisplayName("실패: 같은 보석 2개를 가져갈 때, 해당 보석이 4개 미만이면 실패해야 한다. (사용자 요구 조건)")
    void takeTokens_ShouldFail_WhenTakingTwoSameTokensAndLessThanFourAvailable() {
        // Given
        // 테스트를 위해 초기 상태에서 ONYX 토큰 개수를 3개로 설정 (4개 미만)
        initialGameState.getBoardStateDto().getAvailableTokens().put(ONYX, 3);
        when(gameStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(initialGameState));

        Map<GemType, Integer> tokensToTake = Map.of(ONYX, 2);
        TakeTokenRequestDto request = new TakeTokenRequestDto(TEST_ROOM_ID, HOST_ID, tokensToTake);

        // When & Then
        // INVALID_TWO_TOKEN_RULE 예외가 발생하는지 확인
        assertThrows(IllegalArgumentException.class, () -> {
            playGameService.takeTokens(request);
        }, ErrorCode.INVALID_TWO_TOKEN_RULE.getMessage());

        // save가 호출되지 않았는지 검증
        verify(gameStateRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("실패: 플레이어 토큰이 10개를 초과할 경우 예외가 발생해야 한다. (사용자 요구 조건)")
    void takeTokens_ShouldFail_WhenPlayerTokenLimitExceeded() {
        // Given
        // 호스트의 현재 토큰을 8개로 설정
        PlayerStateDto hostState = initialGameState.getPlayerStateDto().stream()
                .filter(p -> p.getPlayer().getPlayerId().equals(HOST_ID))
                .findFirst().orElseThrow();
        hostState.getTokens().put(DIAMOND, 3);
        hostState.getTokens().put(SAPPHIRE, 3);
        hostState.getTokens().put(EMERALD, 2); // 총 8개

        when(gameStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(initialGameState));

        // 추가로 3개를 가져가는 요청 (총 8 + 3 = 11개)
        // [수정]: GOLD를 제외하고 유효한 토큰 3개를 요청 (RUBY, ONYX, EMERALD)
        Map<GemType, Integer> tokensToTake = Map.of(RUBY, 1, ONYX, 1, EMERALD, 1);
        TakeTokenRequestDto request = new TakeTokenRequestDto(TEST_ROOM_ID, HOST_ID, tokensToTake);

        // When & Then
        // PLAYER_TOKEN_LIMIT_EXCEEDED 예외(IllegalStateException)가 발생하는지 확인
        assertThrows(IllegalStateException.class, () -> {
            playGameService.takeTokens(request);
        }, ErrorCode.PLAYER_TOKEN_LIMIT_EXCEEDED.getMessage());

        // 예외가 발생했으므로 save가 호출되지 않았는지 검증
        verify(gameStateRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("실패: 보드에 충분한 토큰이 없어 획득에 실패해야 한다.")
    void takeTokens_ShouldFail_WhenNotEnoughBoardToken() {
        // Given
        // 테스트를 위해 초기 상태에서 EMERALD 토큰 개수를 0개로 설정
        initialGameState.getBoardStateDto().getAvailableTokens().put(EMERALD, 0);
        when(gameStateRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(initialGameState));

        // EMERALD 1개를 가져가려는 요청
        Map<GemType, Integer> tokensToTake = Map.of(EMERALD, 1, DIAMOND, 1, RUBY, 1);
        TakeTokenRequestDto request = new TakeTokenRequestDto(TEST_ROOM_ID, HOST_ID, tokensToTake);

        // When & Then
        // NOT_ENOUGH_BOARD_TOKEN 예외가 발생하는지 확인
        assertThrows(IllegalArgumentException.class, () -> {
            playGameService.takeTokens(request);
        }, ErrorCode.NOT_ENOUGH_BOARD_TOKEN.getMessage());

        // save가 호출되지 않았는지 검증
        verify(gameStateRepository, times(0)).save(any());
    }
}