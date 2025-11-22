package com.splendor.project.domain.game.dto.response;

import com.splendor.project.domain.data.GemType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class ResponseTokenDto {

    private Map<GemType , Integer> token ;
}
