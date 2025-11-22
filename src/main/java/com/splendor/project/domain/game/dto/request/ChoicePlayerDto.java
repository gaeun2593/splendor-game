package com.splendor.project.domain.game.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
@Data
@AllArgsConstructor
public class ChoicePlayerDto {
    private String currentTurnId;
    private Long roomId;
    private String splendorAction;


}