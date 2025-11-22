package com.splendor.project.domain.game.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@RedisHash("cardSelection") // Redis Key: cardSelection:{roomId}
public class SelectionCardStateDto implements Serializable {

    @org.springframework.data.annotation.Id
    private Long roomId;

    private String playerId;

    // 현재 선택된 카드의 ID. null이면 선택된 카드 없음.
    private Integer cardIdToBuy;

    public SelectionCardStateDto(Long roomId, String playerId) {
        this.roomId = roomId;
        this.playerId = playerId;
    }
}