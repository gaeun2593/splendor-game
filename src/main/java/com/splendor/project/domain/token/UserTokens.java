package com.splendor.project.domain.token;
import com.splendor.project.domain.game.entity.GamePlayer;
import jakarta.persistence.*;

@Entity
public class UserTokens {

    @Id
    @GeneratedValue
    private Long userTokenId ;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tokenId")
    private Token tokens;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gamePlayerId")
    private GamePlayer gamePlayers;

    private int count ;

}
