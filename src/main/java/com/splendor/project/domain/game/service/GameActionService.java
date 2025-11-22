package com.splendor.project.domain.game.service;

import com.splendor.project.domain.data.entity.StaticCard;
import com.splendor.project.domain.data.repository.StaticCardRepository;
import com.splendor.project.domain.game.dto.request.BuyCardRequest;
import com.splendor.project.domain.game.entity.GameSession;
import com.splendor.project.domain.game.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GameActionService {

    private final GameRepository gameRedisRepository;
    private final StaticCardRepository staticCardRepository;

    @Transactional
    public void buyCard(BuyCardRequest request) {
        // 1. Redis에서 게임 세션 로드
        GameSession game = gameRedisRepository.findById(request.getRoomId())
                 .orElseThrow(() -> new IllegalArgumentException("진행 중인 게임이 없습니다."));

        // 2. DB에서 카드 정보 조회 (비용 정보 등)
        StaticCard card = staticCardRepository.findById(request.getCardId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카드입니다."));

        // 3. 도메인 로직 실행
        game.buyCard(request.getPlayerId(), card, request.getGoldTokensToUse());

        // 4. 변경된 상태 Redis에 저장
        gameRedisRepository.save(game);

        // 5. 결과 반환 (WebSocket 전송용 DTO)
    }
}