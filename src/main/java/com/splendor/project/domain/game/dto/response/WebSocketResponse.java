package com.splendor.project.domain.game.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketResponse<T> implements Serializable {

    // "SUCCESS" 또는 "ERROR"
    private String status;

    // 성공 시 전달할 실제 데이터 DTO (예: GameStateDto, Map<GemType, Integer>)
    private T data;

    // 에러 시 전달할 메시지 (성공 시 null)
    private String message;

    // --- 팩토리 메서드 ---

    // 성공 응답 생성 (데이터 포함)
    public static <T> WebSocketResponse<T> success(T data) {
        return new WebSocketResponse<>("SUCCESS", data, null);
    }

    // 에러 응답 생성 (메시지 포함)
    public static <T> WebSocketResponse<T> error(String message) {
        return new WebSocketResponse<>("ERROR", null , message);
    }
}