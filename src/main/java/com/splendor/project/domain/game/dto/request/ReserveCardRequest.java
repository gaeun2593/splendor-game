package com.splendor.project.domain.game.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReserveCardRequest {
    private Long roomId;
    private String playerId;
    private Long cardId; // 예약할 카드 ID (null이면 덱에서 랜덤 예약)
    // 만약 토큰이 10개를 초과해서 버려야 한다면, 버릴 토큰 정보도 여기에 포함될 수 있음
}
