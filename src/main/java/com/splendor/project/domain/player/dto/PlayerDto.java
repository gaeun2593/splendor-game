package com.splendor.project.domain.player.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
public class PlayerDto {
    private String nickname;

    @JsonProperty("isReady")
    private boolean ready ;
}
