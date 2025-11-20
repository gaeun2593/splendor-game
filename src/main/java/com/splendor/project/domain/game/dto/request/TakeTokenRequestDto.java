package com.splendor.project.domain.game.dto.request;

import com.splendor.project.domain.data.GemType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TakeTokenRequestDto {

    private Long roomId; // 현재 게임 ID (Redis Key)

    private String playerId; // 토큰을 가져가는 플레이어 ID

    // 플레이어가 가져가려는 보석 타입과 개수.
    // 예: 3개 획득 시 {DIAMOND: 1, EMERALD: 1, RUBY: 1}
    // 예: 2개 획득 시 {SAPPHIRE: 2}
    private Map<GemType, Integer> tokensToTake;

}
