package com.splendor.project.domain.game.service;

import com.splendor.project.domain.game.dto.response.BoardStateDto;
import com.splendor.project.domain.game.dto.response.GameStateDto;
import com.splendor.project.domain.game.repository.GameStateRepository;
import com.splendor.project.domain.player.entity.Player;
import com.splendor.project.domain.room.entity.Room;
import com.splendor.project.domain.room.entity.RoomStatus;
import com.splendor.project.domain.room.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.splendor.project.domain.data.GemType.*;
import static org.assertj.core.api.Assertions.assertThat;
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
    private Room testRoom;
    private Player hostPlayer;
    private Player guestPlayer;
    private BoardStateDto mockBoardState;

    // Reflection 사용으로 throws Exception 추가
    @BeforeEach
    void setUp() throws Exception {
        // 1. Room 객체를 생성하고 ID를 수동으로 설정 (JPA가 ID를 생성하지 않기 때문에 필수)
        testRoom = new Room("Test Room", RoomStatus.WAITING);

        // [수정된 부분] 리플렉션을 사용하여 private roomId 필드에 TEST_ROOM_ID 값을 주입
        setRoomId(testRoom, TEST_ROOM_ID);

        // 2. Player 생성 (Player 생성자가 자동으로 testRoom.getPlayers()에 추가함)
        hostPlayer = new Player(testRoom, "HostName", true, true);
        guestPlayer = new Player(testRoom, "GuestName", true, true);

        // 3. BoardStateDto Mock 설정
        mockBoardState = new BoardStateDto(
                List.of(), List.of(), Map.of(DIAMOND, 4, SAPPHIRE, 4, RUBY, 4, EMERALD, 4, ONYX, 4, GOLD, 5)
        );
    }

    /**
     * JPA의 @GeneratedValue를 사용하는 엔티티의 ID를 테스트 코드에서 수동으로 설정하기 위한 리플렉션 헬퍼 메서드.
     */
    private void setRoomId(Room room, Long id) throws Exception {
        // Room 클래스에서 private 필드인 "roomId"를 가져옴
        Field field = Room.class.getDeclaredField("roomId");

        // private 필드에 접근 가능하도록 설정
        field.setAccessible(true);

        // room 객체의 roomId 필드에 id 값을 설정
        field.set(room, id);
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
}