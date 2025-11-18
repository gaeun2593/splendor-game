package com.splendor.project.domain.game.dto.response;

import com.splendor.project.domain.data.GemType;
import com.splendor.project.domain.data.StaticCard;
import com.splendor.project.domain.data.StaticNoble;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class BoardStateDto {
    private List<List<StaticCard>> cards ;
    private List<StaticNoble> nobles ;
    private Map<GemType, Integer> availableTokens;
}
