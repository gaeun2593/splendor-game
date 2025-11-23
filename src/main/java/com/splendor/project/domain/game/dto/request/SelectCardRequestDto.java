package com.splendor.project.domain.game.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SelectCardRequestDto {
    private Long roomId;
    private String playerId;
    private int cardId; // 선택/취소된 카드의 ID
    private boolean isSelected; // 선택: true, 취소: false
}