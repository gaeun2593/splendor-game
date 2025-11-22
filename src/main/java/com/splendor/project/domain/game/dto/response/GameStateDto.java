package com.splendor.project.domain.game.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@RedisHash("game")
public class GameStateDto implements Serializable {

    private BoardStateDto boardStateDto;
    private List<PlayerStateDto> playerStateDto;

    @org.springframework.data.annotation.Id // Redis Key의 Suffix로 사용 (예: 방 ID가 1이라면 Redis Key는 game:1)
    private Long gameId;

    private GamePlayerDto currentPlayer;

    // 게임이 종료되었는지 여부
    private boolean isGameOver = false;

    // 최종 승자 (게임이 끝나지 않았다면 null)
    private GamePlayerDto winner;

    // 15점 이상 플레이어가 나왔는지 여부
    private boolean isFinalRound = false;

    // 게임 시작 플레이어 ID (최종 턴 종료 시점 파악을 위함)
    private String startingPlayerId; // gameStart 시 players.get(0).getPlayerId()로 초기화
}
