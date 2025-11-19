package com.splendor.project.domain.game.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GamePlayerDto {
    private String playerName ;
    private String playerId ;
}
