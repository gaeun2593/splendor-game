package com.splendor.project.domain.user.service;

import com.splendor.project.domain.user.dto.UserDto;
import com.splendor.project.domain.user.entity.User;
import com.splendor.project.domain.user.repository.UserRepository;
import com.splendor.project.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 닉네임 기반 간편 가입
     */
    @Transactional
    public UserDto.JoinResponseDto join(UserDto.JoinRequestDto requestDto) {

        // 1. 닉네임 중복 검사
        userRepository.findByNickname(requestDto.getNickname())
                .ifPresent(user -> {
                    throw new IllegalArgumentException("이미 사용중인 닉네임입니다.");
                });

        // 2. 유저 생성 및 RDB에 저장
        User newUser = new User(requestDto.getNickname());
        userRepository.save(newUser);

        // 3. JWT 토큰 생성
        String jwtToken = jwtTokenProvider.createToken(newUser.getId(), newUser.getNickname());

        // 4. DTO로 변환하여 응답
        return new UserDto.JoinResponseDto(newUser.getId(), newUser.getNickname(), jwtToken);
    }
}
