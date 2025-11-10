package com.splendor.project.domain.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "static_nobles")
public class StaticNoble {

    @Id
    private Long id;
    private int points; // (항상 3점)

    // 획득 조건 (보너스 카드 개수)
    private int costRuby;
    private int costSapphire;
    private int costEmerald;
    private int costOnyx;
    private int costDiamond;
}