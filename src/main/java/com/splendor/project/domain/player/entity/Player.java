package com.splendor.project.domain.player.entity;

import com.splendor.project.domain.room.entity.Room;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "players")
public class Player {

    @JoinColumn(name = "room_id")
    @ManyToOne(fetch = FetchType.LAZY)
    Room room;

    @Id
    @Column(columnDefinition = "VARCHAR(36)") // UUID를 문자열로 저장
    private String playerId; // (PK) UUID를 문자열로 저장

    @Column(nullable = false, unique = true)
    private String nickname; // 유저 닉네임 (중복 불가) // 방장 이름

    private boolean isHosted ;

    private boolean isReady ;

    public Player(Room room, String nickname, boolean isHosted , boolean isReady) {
        this.playerId = UUID.randomUUID().toString();
        this.room = room;
        room.getPlayers().add(this) ;
        this.nickname = nickname;
        this.isHosted = isHosted;
        this.isReady = isReady;
    }

    public void toggleReady(){
        this.isReady = !this.isReady;
    }
}
