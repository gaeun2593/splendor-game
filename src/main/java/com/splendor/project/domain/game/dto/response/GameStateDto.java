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
}
