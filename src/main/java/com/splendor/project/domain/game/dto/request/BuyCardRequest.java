package com.splendor.project.domain.game.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BuyCardRequest {

    private Long roomId;
    private String playerId;
    private Long cardId;
    private int goldTokensToUse; // 황금 토큰 사용 개수 (선택)

}