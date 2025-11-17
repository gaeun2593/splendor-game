package com.splendor.project.domain.game;

import com.splendor.project.domain.data.entity.StaticCard;
import com.splendor.project.domain.game.entity.GameSession;
import com.splendor.project.domain.game.entity.GemType;
import com.splendor.project.domain.game.entity.PlayerState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameDomainTest {

    // 도우미 메서드: 더미 카드 생성 (비용 설정 편의용)
    private StaticCard createCard(Long id, int diamond, int sapphire, int emerald, int ruby, int onyx) {
        return new StaticCard(id, 1, 0, "DIAMOND", ruby, sapphire, emerald, onyx, diamond);
    }

    @Test
    @DisplayName("토큰이 충분하면 카드를 구매하고 토큰이 차감된다")
    void buyCardSuccess() {
        // given
        GameSession game = new GameSession(1L);
        PlayerState player = new PlayerState("p1", "User1");

        // 플레이어에게 다이아몬드 5개 지급
        Map<GemType, Integer> tokens = new HashMap<>(player.getTokens());
        tokens.put(GemType.DIAMOND, 5);
        player.setTokens(tokens);

        game.addPlayer(player);
        game.setCurrentPlayerId("p1");

        // 비용: 다이아 3개짜리 카드
        StaticCard card = createCard(1L, 3, 0, 0, 0, 0);

        // when
        game.buyCard("p1", card, 0);

        // then
        // 1. 토큰이 5개 -> 2개로 줄었는지 확인
        assertThat(player.getTokens().get(GemType.DIAMOND)).isEqualTo(2);
        // 2. 구매한 카드 목록에 추가되었는지 확인
        assertThat(player.getPurchasedCards()).hasSize(1);
        assertThat(player.getPurchasedCards().get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("보석이 부족하면 구매에 실패한다")
    void buyCardFail_NotEnoughTokens() {
        // given
        GameSession game = new GameSession(1L);
        PlayerState player = new PlayerState("p1", "User1");

        // 플레이어에게 다이아몬드 1개 지급 (부족함)
        Map<GemType, Integer> tokens = new HashMap<>(player.getTokens());
        tokens.put(GemType.DIAMOND, 1);
        player.setTokens(tokens);

        game.addPlayer(player);
        game.setCurrentPlayerId("p1");

        StaticCard card = createCard(1L, 3, 0, 0, 0, 0); // 비용 3개

        // when & then
        assertThatThrownBy(() -> game.buyCard("p1", card, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("비용이 부족합니다");
    }

    @Test
    @DisplayName("기존에 구매한 카드의 할인(보너스)이 적용되어야 한다")
    void buyCardWithDiscount() {
        // given
        GameSession game = new GameSession(1L);
        PlayerState player = new PlayerState("p1", "User1");

        // 이미 다이아몬드 보너스 카드 2장을 가지고 있음
        StaticCard bonusCard1 = new StaticCard(10L, 1, 0, "DIAMOND", 0,0,0,0,0);
        StaticCard bonusCard2 = new StaticCard(11L, 1, 0, "DIAMOND", 0,0,0,0,0);
        player.addCard(bonusCard1);
        player.addCard(bonusCard2);

        // 토큰은 다이아몬드 1개만 있음
        Map<GemType, Integer> tokens = new HashMap<>(player.getTokens());
        tokens.put(GemType.DIAMOND, 1);
        player.setTokens(tokens);

        game.addPlayer(player);
        game.setCurrentPlayerId("p1");

        // 목표 카드 비용: 다이아 3개
        // 필요 비용: 3 - 2(할인) = 1개 -> 1개 보유중이므로 구매 가능해야 함
        StaticCard targetCard = createCard(1L, 3, 0, 0, 0, 0);

        // when
        game.buyCard("p1", targetCard, 0);

        // then
        assertThat(player.getPurchasedCards()).hasSize(3); // 기존2 + 신규1
        assertThat(player.getTokens().get(GemType.DIAMOND)).isEqualTo(0); // 1개 사용됨
    }

    @Test
    @DisplayName("카드를 예약하면 예약 목록에 추가되고 황금 토큰을 1개 받는다")
    void reserveCardSuccess() {
        // given
        PlayerState player = new PlayerState("p1", "User1");
        StaticCard card = new StaticCard(1L, 1, 0, "RUBY", 0,0,0,0,0);

        // 초기 황금 토큰 0개
        player.getTokens().put(GemType.GOLD, 0);

        // when
        // 황금 토큰 지급 여부는 GameSession이 결정해서 넘겨준다고 가정 (은행 재고 확인 후)
        player.reserveCard(card, true);

        // then
        assertThat(player.getReservedCards()).hasSize(1);
        assertThat(player.getReservedCards()).contains(card);
        assertThat(player.getTokens().get(GemType.GOLD)).isEqualTo(1);
    }

    @Test
    @DisplayName("예약한 카드가 3장 꽉 찼으면 더 이상 예약할 수 없다")
    void reserveCardFail_MaxLimit() {
        // given
        PlayerState player = new PlayerState("p1", "User1");
        // 이미 3장 예약함
        player.reserveCard(new StaticCard(1L, 1, 0, "RUBY", 0,0,0,0,0), false);
        player.reserveCard(new StaticCard(2L, 1, 0, "RUBY", 0,0,0,0,0), false);
        player.reserveCard(new StaticCard(3L, 1, 0, "RUBY", 0,0,0,0,0), false);

        StaticCard newCard = new StaticCard(4L, 1, 0, "RUBY", 0,0,0,0,0);

        // when & then
        assertThatThrownBy(() -> player.reserveCard(newCard, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("예약은 최대 3장까지만");
    }

    @Test
    @DisplayName("은행에 황금 토큰이 없으면 카드는 예약되지만 황금 토큰은 받지 못한다")
    void reserveCardWithoutGold() {
        // given
        GameSession game = new GameSession(1L);
        PlayerState player = new PlayerState("p1", "User1");
        game.addPlayer(player);
        game.setCurrentPlayerId("p1");

        // 은행의 황금 토큰을 0개로 설정 (테스트 편의상 map 조작 가정)
        game.getBoardTokens().put(GemType.GOLD, 0);

        StaticCard card = new StaticCard(1L, 1, 0, "RUBY", 0,0,0,0,0);

        // when
        game.reserveCard("p1", card);

        // then
        assertThat(player.getReservedCards()).hasSize(1); // 예약은 성공
        assertThat(player.getTokens().get(GemType.GOLD)).isEqualTo(0); // 토큰은 못 받음
    }
}