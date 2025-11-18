package com.splendor.project.domain.game.service;

import com.splendor.project.domain.data.GemType;
import com.splendor.project.domain.data.StaticCard;
import com.splendor.project.domain.data.StaticNoble;
import com.splendor.project.domain.game.dto.response.BoardStateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.splendor.project.domain.data.GemType.*;
import static com.splendor.project.domain.data.GemType.DIAMOND;
import static com.splendor.project.domain.data.GemType.RUBY;

@Service
@RequiredArgsConstructor
@Transactional
public class InitialGameService {

    private final GameStaticDataLoader staticDataLoader;

    public BoardStateDto initializeGame() {
        int drawCount = 4;

        // Level 1 카드 초기화
        List<StaticCard> initialL1Cards = initializeLevelCards(1, drawCount);

        // Level 2 카드 초기화
        List<StaticCard> initialL2Cards = initializeLevelCards(2, drawCount);

        // Level 3 카드 초기화
        List<StaticCard> initialL3Cards = initializeLevelCards(3, drawCount);

        // 귀족 타일 초기화 (중복이 없으므로 그대로 유지)
        List<StaticNoble> allNobles = staticDataLoader.getAllNobles();

        List<List<StaticCard>> cards = new ArrayList<>();
        cards.add(initialL1Cards);
        cards.add(initialL2Cards);
        cards.add(initialL3Cards);

        BoardStateDto boardStateDto = new BoardStateDto(cards, allNobles, Map.of(DIAMOND , 4  , SAPPHIRE,4, RUBY ,4 , EMERALD,4  , ONYX,4 , GOLD ,5 )) ;
        return boardStateDto ;


    }


    private List<StaticCard> initializeLevelCards(int level, int count) {
        List<StaticCard> shuffledList = staticDataLoader.getShuffledLevelCards(level);

        // 2. 덱을 Deque(양방향 큐)으로 변환
        Deque<StaticCard> deck = new LinkedList<>(shuffledList);

        // 3. 초기 카드를 뽑을 리스트
        List<StaticCard> initialCards = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            // poll() : 덱의 맨 앞 요소(top card)를 가져오고 제거합니다.
            StaticCard card = deck.poll();

            if (card != null) {
                initialCards.add(card);
            }
        }


        return initialCards;
    }
}


