package com.splendor.project.domain.user.controller;

import com.splendor.project.domain.user.dto.UserDto;
import com.splendor.project.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 가입을 위한 HTTP API 컨트롤러 (HTTP 통신)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    /**
     * "간편 닉네임 가입" 엔드포인트
     * @param requestDto ({"nickname": "유저닉네임"})
     * @return ({"userId": "...", "nickname": "...", "jwtToken": "Bearer ..."})
     */
    @PostMapping("/join")
    public ResponseEntity<UserDto.JoinResponseDto> join(@RequestBody UserDto.JoinRequestDto requestDto) {
        UserDto.JoinResponseDto responseDto = userService.join(requestDto);
        return ResponseEntity.ok(responseDto);
    }

    // 로그인 기능 따로?
    // POST /api/login 을 만들고, 닉네임으로 유저를 찾아서 JWT만 새로 발급해주면 됩니다.
}
