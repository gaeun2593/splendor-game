package com.splendor.project.domain.game.dto.request;

import com.splendor.project.domain.data.GemType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SelectTokenRequestDto {

    private Long roomId;

    // 현재 턴을 진행 중인 플레이어 ID (검증용)
    private String playerId;

    // 클라이언트가 클릭한 토큰 타입
    private GemType token;

    // 해당 토큰을 '선택'했는지 '취소'했는지 구분하는 플래그 (선택: true, 취소: false)
    //private boolean isSelect;

    private SelectStatus selectStatus ;
}
