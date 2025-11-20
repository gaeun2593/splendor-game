package com.splendor.project.domain.game.dto.response;

import com.splendor.project.domain.data.GemType;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@RedisHash("selectedToken") // Redis Key: selectedToken:{roomId}
public class SelectTokenStateDto implements Serializable {

    @org.springframework.data.annotation.Id
    private Long roomId; // 게임 ID

    // 현재 턴 플레이어의 ID
    private String playerId;

    // 현재까지 선택된 토큰 목록 및 수량. 예: {DIAMOND: 1, SAPPHIRE: 1}
    private Map<GemType, Integer> tokensToTake = new HashMap<>();

    // 토큰 획득 행동이 완료되었는지 여부
    private boolean isActionCompleted = false;

    // 턴 플레이어 초기화 시 사용
    public SelectTokenStateDto(Long roomId, String playerId) {
        this.roomId = roomId;
        this.playerId = playerId;
    }
}