package com.splendor.project.domain.game.dto.response;

import com.splendor.project.domain.data.GemType;
import com.splendor.project.domain.player.entity.Player;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class PlayerStateDto {
    private GamePlayerDto player ;
    private int score;  // 총 점수 (개발 카드 + 귀족)
    private Map<GemType , Integer> tokens ;  // 보유 중인 보석 토큰
    private Map<GemType , Integer> bonuses ;  // 보유한 개발 카드로 인한 총 할인 보너스
   // private List<CardDto> reservedCards ;  // 킵(예약)한 카드 목록 (최대 3장)
  //  private List<NobleDto> nobles ; // 획득한 귀족 타일 목록

    // 동점 시 1순위
    private int purchasedCardCount;

    // 동점 시 2순위
    private int nobleCount; // 획득한 귀족 타일 수

    //  후공 플레이어 승리 룰을 위한 턴 순서 인덱스
    private int turnOrder;



}
