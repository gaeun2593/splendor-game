package com.splendor.project;

import com.splendor.project.domain.data.entity.StaticCard;
import com.splendor.project.domain.data.entity.StaticNoble;
import com.splendor.project.domain.data.repository.StaticCardRepository;
import com.splendor.project.domain.data.repository.StaticNobleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
class StaticDataRepositoryTest {
    @Autowired
    private StaticCardRepository staticCardRepository;

    @Autowired
    private StaticNobleRepository staticNobleRepository;

    @Test
    @DisplayName("data.sql의 카드 데이터가 H2 DB에 로드되고, JPA로 조회되어야 한다.")
    void 개발_카드_로드_테스트() {
        // when (카드 모두 조회)
        List<StaticCard> allCards = staticCardRepository.findAll();

        // then (비어있는지 확인)
        assertThat(allCards).isNotEmpty();

        assertThat(allCards.size()).isEqualTo(90);

        // 샘플 데이터(ID=1) 검증
        StaticCard card1 = staticCardRepository.findById(1L).orElseThrow();
        assertThat(card1.getBonusGem()).isEqualTo("DIAMOND");
        assertThat(card1.getCostSapphire()).isEqualTo(1);

        System.out.println("--- [성공] Static Cards 로드 완료. 총 " + allCards.size() + "장 ---");
    }

    @Test
    @DisplayName("data.sql의 귀족 데이터가 H2 DB에 로드되고, JPA로 조회되어야 한다.")
    void 귀족_카드_로드_테스트() {
        // when (카드 모두 조회)
        List<StaticNoble> allNobles = staticNobleRepository.findAll();

        // then (비어있는지 확인)
        assertThat(allNobles).isNotEmpty();
        assertThat(allNobles.size()).isEqualTo(10);

        // 샘플 데이터(ID=101) 검증
        StaticNoble noble101 = staticNobleRepository.findById(101L).orElseThrow();
        assertThat(noble101.getPoints()).isEqualTo(3);
        assertThat(noble101.getCostEmerald()).isEqualTo(4);

        System.out.println("--- [성공] Static Nobles 로드 완료. 총 " + allNobles.size() + "명 ---");
    }
}
