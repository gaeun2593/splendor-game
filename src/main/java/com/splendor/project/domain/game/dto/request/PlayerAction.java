package com.splendor.project.domain.game.dto.request;

import java.util.HashMap;
import java.util.Map;

public enum PlayerAction {
    TAKE_TOKENS("보석 토큰을 가져오는 액션을 선택했습니다."),
    BUY_CARD("개발 카드 1장을 구입하는 액션을 선택했습니다."),
    RESERVE_CARD("개발 카드 1장을 보관하고 황금 토큰 1개를 받는 액션을 선택했습니다.");


    private final String message;

    PlayerAction(String message) {
        this.message = message;
    }


    private static final Map<String, PlayerAction> MESSAGE_MAP = new HashMap<>();

    static {
        for (PlayerAction action : PlayerAction.values()) {
            MESSAGE_MAP.put(action.message, action);
        }
    }

    public static PlayerAction fromMessage(String message) {
        return MESSAGE_MAP.get(message);
    }
}