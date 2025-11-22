package com.splendor.project.domain.game.logic;

import com.splendor.project.domain.data.GemType;
import com.splendor.project.domain.data.StaticCard;
import com.splendor.project.domain.game.dto.response.PlayerStateDto;
import com.splendor.project.exception.ErrorCode;
import com.splendor.project.exception.GameLogicException;

import java.util.HashMap;
import java.util.Map;

public class PlayerStateCalculator {

    /**
     * 플레이어의 현재 토큰과 보너스를 고려하여 카드를 구매할 수 있는지 검증하고, 지불 토큰을 계산합니다.
     *
     * @param playerStateDto 현재 플레이어 상태 (토큰, 보너스)
     * @param card 구매하려는 카드
     * @return 구매에 필요한 지불해야 할 토큰 맵 (GemType, Count) - 황금 토큰 사용량 포함
     * @throws GameLogicException 구매가 불가능할 경우 발생
     */
    public static Map<GemType, Integer> calculatePayment(PlayerStateDto playerStateDto, StaticCard card) {
        Map<GemType, Integer> requiredPayment = new HashMap<>();
        int totalGoldTokenNeeded = 0;

        for (GemType gem : GemType.values()) {
            if (gem == GemType.GOLD) continue;

            int cost = getCardCost(card, gem);
            if (cost == 0) continue;

            // 1. 보너스(할인) 적용 후 순수하게 토큰으로 지불해야 할 금액
            int bonus = playerStateDto.getBonuses().getOrDefault(gem, 0);
            int netCost = Math.max(0, cost - bonus);

            if (netCost == 0) continue; // 보너스로 모두 충당 가능

            // 2. 일반 토큰 잔액 확인
            int currentTokens = playerStateDto.getTokens().getOrDefault(gem, 0);

            if (currentTokens < netCost) {
                // 3. 일반 토큰이 부족하면 황금 토큰으로 대체할 수 있는지 확인
                int neededFromGold = netCost - currentTokens;
                totalGoldTokenNeeded += neededFromGold;
                requiredPayment.put(gem, currentTokens); // 가진 일반 토큰 모두 지불
            } else {
                // 4. 일반 토큰으로 충분하면 일반 토큰만 지불
                requiredPayment.put(gem, netCost);
            }
        }

        // 5. 최종 황금 토큰 사용 가능 여부 검증
        int availableGold = playerStateDto.getTokens().getOrDefault(GemType.GOLD, 0);

        if (availableGold < totalGoldTokenNeeded) {
            // 1. 검증 실패: 황금 토큰까지 부족하면 예외 발생
            throw new GameLogicException(ErrorCode.NOT_ENOUGH_TOKENS);
        }

        if (totalGoldTokenNeeded > 0) {
            requiredPayment.put(GemType.GOLD, totalGoldTokenNeeded);
        }

        return requiredPayment;
    }

    private static int getCardCost(StaticCard card, GemType gem) {
        return switch (gem) {
            case DIAMOND -> card.costDiamond();
            case SAPPHIRE -> card.costSapphire();
            case EMERALD -> card.costEmerald();
            case RUBY -> card.costRuby();
            case ONYX -> card.costOnyx();
            default -> 0;
        };
    }
}