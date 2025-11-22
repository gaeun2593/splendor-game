// src/main/java/com/splendor/project/domain/game/service/TokenAcquisitionValidator.java (업데이트)
package com.splendor.project.domain.game.service;

import com.splendor.project.domain.data.GemType;
import com.splendor.project.exception.ErrorCode;
import com.splendor.project.exception.GameLogicException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

import static com.splendor.project.domain.data.GemType.GOLD;

@Component
@RequiredArgsConstructor
public class TokenAcquisitionValidator {

    /**
     * 토큰이 하나씩 추가될 때마다 현재까지 선택된 토큰 목록의 유효성을 검사합니다. (부분 검증)
     */
    public void validatePartialTokenAcquisition(Map<GemType, Integer> tokensToTake, Map<GemType, Integer> availableTokens) {
        // 0. 골드 토큰 요청 검증 (일반 획득 시 선택 불가)
        if (tokensToTake.containsKey(GOLD) && tokensToTake.get(GOLD) > 0) {
            throw new GameLogicException(ErrorCode.INVALID_TOKEN_ACTION);
        }

        // 유효한 선택만 필터링 (개수 0인 토큰 제외)
        Map<GemType, Integer> validTokensToTake = tokensToTake.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // 1. 총 개수 3개 초과 검증
        int totalTakeCount = validTokensToTake.values().stream().mapToInt(Integer::intValue).sum();
        if (totalTakeCount > 3) {
            throw new GameLogicException(ErrorCode.INVALID_TOKEN_ACTION);
        }

        // 2. 종류 개수 검증 (최대 3종류)
        int distinctGemCount = validTokensToTake.size();
        if (distinctGemCount > 3) {
            throw new GameLogicException(ErrorCode.TOO_MANY_TOKEN_TYPES);
        }

        // 3. 2개 획득 규칙 검증
        if (distinctGemCount == 1) {
            if (totalTakeCount == 2) {
                GemType gemType = validTokensToTake.keySet().iterator().next();
                Integer availableCount = availableTokens.getOrDefault(gemType, 0);

                // 같은 보석 2개 획득 시 4개 이상 남아있어야 함
                if (availableCount < 4) {
                    throw new GameLogicException(ErrorCode.INVALID_TWO_TOKEN_RULE);
                }
            } else if (totalTakeCount > 2) {
                // 한 종류를 3개 이상 선택 시도 (불가능한 행동)
                throw new GameLogicException(ErrorCode.INVALID_TOKEN_ACTION);
            }
        } else if (distinctGemCount >= 2) {
            // 4. 서로 다른 종류 토큰 획득 시 개수 검증 (각각 1개만 가능)
            if (validTokensToTake.values().stream().anyMatch(count -> count > 1)) {
                throw new GameLogicException(ErrorCode.INVALID_TOKEN_ACTION);
            }
        }

        // 5. 보드 재고 검증
        for (Map.Entry<GemType, Integer> entry : validTokensToTake.entrySet()) {
            GemType gemType = entry.getKey();
            int count = entry.getValue();

            Integer availableCount = availableTokens.getOrDefault(gemType, 0);
            if (availableCount < count) {
                throw new GameLogicException(ErrorCode.NOT_ENOUGH_BOARD_TOKEN);
            }
        }
    }

    /**
     * 최종 획득 시 전체 토큰 목록의 유효성을 검사합니다. (턴 종료 시 호출)
     */
    public void validateTokenAcquisition(Map<GemType, Integer> tokensToTake, Map<GemType, Integer> availableTokens) {
        // 0. 가져갈 수 없는 골드 토큰 요청 제외
        if (tokensToTake.containsKey(GOLD) && tokensToTake.get(GOLD) > 0) {
            throw new GameLogicException(ErrorCode.INVALID_TOKEN_ACTION);
        }

        // 가져가려는 보석 종류만 필터링
        Map<GemType, Integer> validTokensToTake = tokensToTake.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        int totalTakeCount = validTokensToTake.values().stream().mapToInt(Integer::intValue).sum();
        int distinctGemCount = validTokensToTake.size();

        // 1. 가져가려는 토큰의 총 개수 확인 (스플렌더 기본 룰: 최대 3개)
        if (totalTakeCount > 3 || totalTakeCount <= 0) {
            throw new GameLogicException(ErrorCode.INVALID_TOKEN_ACTION);
        }

        // 2. 획득 행동 유형 검증
        if (distinctGemCount == 1) {
            if (totalTakeCount == 2) {
                GemType gemType = validTokensToTake.keySet().iterator().next();
                Integer availableCount = availableTokens.getOrDefault(gemType, 0);

                // [사용자 요구 규칙] 같은 보석 2개 획득 시 4개 이상 남아있어야 함
                if (availableCount < 4) {
                    throw new GameLogicException(ErrorCode.INVALID_TWO_TOKEN_RULE);
                }
            } else if (totalTakeCount == 3) {
                // B. 한 종류 3개 획득 요청 (불가능한 행동)
                throw new GameLogicException(ErrorCode.INVALID_TOKEN_ACTION);
            }
            // C. 한 종류 1개 획득은 항상 유효 (totalTakeCount = 1)

        } else if (distinctGemCount >= 2 && distinctGemCount <= 3) {
            // D. 서로 다른 종류 토큰 획득 (스플렌더 기본 룰: 최대 3종류, 각 1개씩)
            if (totalTakeCount != distinctGemCount) {
                // 2종류 이상 가져가는데, 각 1개가 아니면 (예: {D:2, S:1}) 유효하지 않음.
                throw new GameLogicException(ErrorCode.INVALID_TOKEN_ACTION);
            }
        } else {
            // 4종류 이상을 시도하거나, 0종류를 시도하는 경우 (이미 validTokensToTake에서 필터링되었지만 안전망)
            throw new GameLogicException(ErrorCode.TOO_MANY_TOKEN_TYPES);
        }

        // 3. 보드 재고 검증
        for (Map.Entry<GemType, Integer> entry : validTokensToTake.entrySet()) {
            GemType gemType = entry.getKey();
            int count = entry.getValue();

            Integer availableCount = availableTokens.getOrDefault(gemType, 0);
            if (availableCount < count) {
                throw new GameLogicException(ErrorCode.NOT_ENOUGH_BOARD_TOKEN);
            }
        }
    }
}