package com.splendor.project.domain.game.dto.request;

import com.splendor.project.domain.data.GemType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SelectedTokenDto {
    private String playerId ;
    private String currentTurnId ;
    private GemType token ;

}
