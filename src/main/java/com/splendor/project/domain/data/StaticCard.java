package com.splendor.project.domain.data;

public record StaticCard(
        int id,
        int level,
        int points,
        GemType bonusGem, // 실제 구현 시 Enum 사용 권장
        int costDiamond,
        int costSapphire,
        int costEmerald,
        int costRuby,
        int costOnyx
) {

}