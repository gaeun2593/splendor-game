package com.splendor.project.domain.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 닉네임 가입 시 사용할 DTO (Request / Response)
 */
public class UserDto {

    /**
     * 클라이언트 -> 서버 (닉네임 가입 요청)
     */
    @Getter
    @Setter // JSON 역직렬화를 위해 Setter 또는 NoArgsConstructor 필요
    @NoArgsConstructor
    public static class JoinRequestDto {
        private String nickname;
    }


    /**
     * 서버 -> 클라이언트 (가입 성공 + JWT 토큰 응답)
     */
    @Getter
    public static class JoinResponseDto {
        private String userId;
        private String nickname;
        private String jwtToken; // (accessToken)

        public JoinResponseDto(String userId, String nickname, String jwtToken) {
            this.userId = userId;
            this.nickname = nickname;
            this.jwtToken = "Bearer " + jwtToken; // Bearer 타입으로 반환
        }
    }
}
