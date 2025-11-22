package com.splendor.project.exception;

public class GameLogicException extends RuntimeException {
    private final ErrorCode errorCode;

    public GameLogicException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}