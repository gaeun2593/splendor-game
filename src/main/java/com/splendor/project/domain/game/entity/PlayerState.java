package com.splendor.project.domain.game.entity;

import com.splendor.project.domain.data.entity.StaticCard;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Redis 저장용
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PlayerState {

    private String playerId;
    private String nickname;
    private Map<GemType, Integer> tokens = new HashMap<>();
    private List<StaticCard> purchasedCards = new ArrayList<>();

    public PlayerState(String playerId, String nickname) {
        this.playerId = playerId;
        this.nickname = nickname;
        // 초기 토큰 0개 설정
        for (GemType type : GemType.values()) {
            this.tokens.put(type, 0);
        }
    }

    public void setTokens(Map<GemType, Integer> tokens) { this.tokens = tokens; }
    public void addCard(StaticCard card) { this.purchasedCards.add(card); }

    /**
     * 카드 구매 메인 로직
     */
    public void buyCard(StaticCard card, int goldToUse) {
        // 1. 비용 검증
        Map<GemType, Integer> costToPay = calculateActualCost(card);

        // 황금 토큰으로 부족분 메우기 계산
        int totalMissing = 0;
        for (GemType type : costToPay.keySet()) {
            int required = costToPay.get(type);
            int has = tokens.getOrDefault(type, 0);
            if (has < required) {
                totalMissing += (required - has);
            }
        }

        if (goldToUse < totalMissing) {
            throw new IllegalArgumentException("비용이 부족합니다 (황금 토큰 부족).");
        }
        if (tokens.getOrDefault(GemType.GOLD, 0) < goldToUse) {
            throw new IllegalArgumentException("보유한 황금 토큰보다 많이 사용할 수 없습니다.");
        }

        // 2. 비용 지불 (토큰 차감)
        payTokens(costToPay, goldToUse);

        // 3. 카드 추가
        this.purchasedCards.add(card);
    }

    /**
     * 할인(보너스)을 적용한 실제 지불 비용 계산
     */
    private Map<GemType, Integer> calculateActualCost(StaticCard card) {
        Map<GemType, Integer> bonuses = getBonuses();
        Map<GemType, Integer> actualCost = new HashMap<>();

        actualCost.put(GemType.DIAMOND, Math.max(0, card.getCostDiamond() - bonuses.getOrDefault(GemType.DIAMOND, 0)));
        actualCost.put(GemType.SAPPHIRE, Math.max(0, card.getCostSapphire() - bonuses.getOrDefault(GemType.SAPPHIRE, 0)));
        actualCost.put(GemType.EMERALD, Math.max(0, card.getCostEmerald() - bonuses.getOrDefault(GemType.EMERALD, 0)));
        actualCost.put(GemType.RUBY, Math.max(0, card.getCostRuby() - bonuses.getOrDefault(GemType.RUBY, 0)));
        actualCost.put(GemType.ONYX, Math.max(0, card.getCostOnyx() - bonuses.getOrDefault(GemType.ONYX, 0)));

        return actualCost;
    }

    /**
     * 현재 보유한 카드들의 보너스 합계 계산
     */
    private Map<GemType, Integer> getBonuses() {
        Map<GemType, Integer> bonuses = new HashMap<>();
        for (StaticCard card : purchasedCards) {
            try {
                GemType type = GemType.valueOf(card.getBonusGem()); // "DIAMOND" -> Enum 변환
                bonuses.put(type, bonuses.getOrDefault(type, 0) + 1);
            } catch (IllegalArgumentException e) {
                // Enum에 없는 문자열일 경우 무시하거나 로그
            }
        }
        return bonuses;
    }

    /**
     * 실제 토큰 차감 로직
     */
    private void payTokens(Map<GemType, Integer> cost, int goldToUse) {
        for (Map.Entry<GemType, Integer> entry : cost.entrySet()) {
            GemType type = entry.getKey();
            int required = entry.getValue();
            int has = tokens.getOrDefault(type, 0);

            if (has >= required) {
                tokens.put(type, has - required);
            } else {
                // 모자란 만큼은 황금 토큰으로 대체됨 (검증 단계에서 확인했으므로 안전)
                tokens.put(type, 0); // 다 씀
            }
        }
        // 황금 토큰 차감
        int currentGold = tokens.getOrDefault(GemType.GOLD, 0);
        tokens.put(GemType.GOLD, currentGold - goldToUse);
    }
}
