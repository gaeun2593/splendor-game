package com.splendor.project.domain.game.entity;

import com.splendor.project.domain.token.UserTokens;
import com.splendor.project.domain.player.entity.Player;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GamePlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long GamePlayerId ;

    @OneToOne
    private Player player;

    @OneToMany(mappedBy = "gamePlayers")
    private List<UserTokens> userTokens;

}
