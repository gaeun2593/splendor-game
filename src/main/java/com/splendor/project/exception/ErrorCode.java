package com.splendor.project.exception;

public enum ErrorCode {
    ROOM_NOT_FOUND("방이 존재하지 않습니다"),
    PLAYER_NOT_FOUND("플레이어가 존재하지 않습니다"),
    NOT_CURRENT_TURN("현재 턴 플레이어가 아닙니다."),

    // 토큰 획득 관련 에러 코드
    INVALID_TOKEN_ACTION("유효하지 않은 토큰 획득 행동입니다."),
    NOT_ENOUGH_BOARD_TOKEN("보드에 충분한 토큰이 남아있지 않습니다."),
    TOO_MANY_TOKEN_TYPES("동시에 가져갈 수 있는 보석 종류는 최대 3개입니다."),
    INVALID_TWO_TOKEN_RULE("같은 보석 2개를 가져가려면 해당 보석이 4개 이상 남아있어야 합니다."), // 사용자 요구 조건

    // 카드 구매 관련 에러 코드
    CARD_NOT_AVAILABLE("선택한 개발 카드를 찾을 수 없거나 구매할 수 없는 상태입니다."),
    NOT_ENOUGH_TOKENS("보석 토큰이 부족하여 카드를 구매할 수 없습니다."),
    NO_CARD_SELECTED_TO_PURCHASE("구매할 카드가 선택되지 않았습니다."),
    ANOTHER_CARD_ALREADY_SELECTED("이미 다른 카드가 선택되어 있습니다.");


    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}