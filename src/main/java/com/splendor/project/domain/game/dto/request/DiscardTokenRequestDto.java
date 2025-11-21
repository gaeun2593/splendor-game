package com.splendor.project.domain.game.dto.request;

import com.splendor.project.domain.data.GemType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscardTokenRequestDto {

    private Long roomId; // 현재 게임 ID

    // 현재 턴을 진행 중인 플레이어 ID (검증용)
    private String playerId;

    // 클라이언트가 버리기로 선택한 토큰 타입
    private GemType token;
}