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

    // 간단한 테스트를 위한 생성자 및 메서드
    public GameSession(Long roomId) { this.roomId = roomId; }
    public void addPlayer(PlayerState player) { this.players.put(player.getPlayerId(), player); }
    public void setCurrentPlayerId(String playerId) { this.currentPlayerId = playerId; }

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
}
