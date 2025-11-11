package com.splendor.project.domain.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(columnDefinition = "VARCHAR(36)") // UUID를 문자열로 저장
    private String id; // (PK) UUID를 문자열로 저장

    @Column(nullable = false, unique = true)
    private String nickname; // 유저 닉네임 (중복 불가)

    public User(String nickname) {
        this.id = UUID.randomUUID().toString(); // 가입 시 고유 ID 자동 생성
        this.nickname = nickname;
    }
}
