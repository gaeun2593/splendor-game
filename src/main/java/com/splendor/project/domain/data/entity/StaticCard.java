package com.splendor.project.domain.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "static_cards")
public class StaticCard {

    @Id
    private Long id;

    private int level; // 1, 2, 3
    private int points; // 0~5점
    private String bonusGem; // 보너스 보석 (DIAMOND, SAPPHIRE 등)

    // 구매 비용
    private int costRuby;
    private int costSapphire;
    private int costEmerald;
    private int costOnyx;
    private int costDiamond;
}
