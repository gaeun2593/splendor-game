package com.splendor.project.exception;

public enum ErrorCode {
    ROOM_NOT_FOUND("방이 존재하지 않습니다"),
    PLAYER_NOT_FOUND("플레이어가 존재하지 않습니다");


    private final String message;

    ErrorCode(String message) {

        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}