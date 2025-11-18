package com.splendor.project.domain.game.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class GameStateDto {
    private BoardStateDto boardStateDto;
    private List<PlayerStateDto> playerStateDto;
    private Long gameId ;
    private GamePlayerDto currentPlayer ;
}
