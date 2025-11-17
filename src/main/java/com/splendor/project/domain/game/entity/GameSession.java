package com.splendor.project.domain.game.entity;

import com.splendor.project.domain.data.entity.StaticCard;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

// Redis 저장용
@Getter
@NoArgsConstructor
public class GameSession {
    private Long roomId;
    private Map<String, PlayerState> players = new HashMap<>();
    private String currentPlayerId;
    // 은행에 있는 토큰들
    private Map<GemType, Integer> boardTokens = new HashMap<>();

    public GameSession(Long roomId) {
        this.roomId = roomId;
        // 게임 시작 시 기본 토큰 세팅 (예: 2인 기준 4개, 황금 5개)
        boardTokens.put(GemType.GOLD, 5);
        // ... 다른 보석 초기화 -> 방에서 어떻게 인원수를 결정하는지 보고 수정해야할 듯
    }
    public void addPlayer(PlayerState player) { this.players.put(player.getPlayerId(), player); }
    public void setCurrentPlayerId(String playerId) { this.currentPlayerId = playerId; }
    // 테스트용 getter
    public Map<GemType, Integer> getBoardTokens() { return boardTokens; }


    public void buyCard(String playerId, StaticCard card, int goldToUse) {
        // 1. 턴 검증
        if (!playerId.equals(currentPlayerId)) {
            throw new IllegalStateException("당신의 턴이 아닙니다.");
        }

        PlayerState player = players.get(playerId);
        if (player == null) throw new IllegalArgumentException("플레이어가 존재하지 않습니다.");

        // 2. 플레이어 구매 로직 위임
        player.buyCard(card, goldToUse);

        // 3. (추후 구현) 보드판에서 카드 제거 및 새 카드 오픈 로직 추가 필요
        // board.removeCard(card.getId());
    }

    public void reserveCard(String playerId, StaticCard card) {
        if (!playerId.equals(currentPlayerId)) {
            throw new IllegalStateException("당신의 턴이 아닙니다.");
        }

        PlayerState player = players.get(playerId);

        // 1. 황금 토큰 지급 가능 여부 확인
        int bankGold = boardTokens.getOrDefault(GemType.GOLD, 0);
        boolean giveGold = false;

        if (bankGold > 0) {
            giveGold = true;
            boardTokens.put(GemType.GOLD, bankGold - 1); // 은행에서 차감
        }

        // 2. 플레이어에게 위임
        player.reserveCard(card, giveGold);

        // 3. (추후 구현) 보드판에서 해당 카드 제거 (Deck에서 가져온게 아니라면)
        // if (isBoardCard(card)) { board.removeCard(card); }
    }
}
