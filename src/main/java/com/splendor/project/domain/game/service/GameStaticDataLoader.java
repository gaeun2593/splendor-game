package com.splendor.project.domain.game.service;

import com.splendor.project.domain.data.StaticCard;
import com.splendor.project.domain.data.StaticNoble;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.splendor.project.domain.data.GemType.*;

@Getter
@Component
public class GameStaticDataLoader {

    private List<StaticCard> allCards;
    private List<StaticNoble> allNobles;

    @PostConstruct
    public void loadData() {
        // ⭐️ 애플리케이션 시작 시 한 번만 실행되며 데이터를 메모리에 로드합니다.
        this.allCards = createCardList();
        this.allNobles = createNobleList();

        System.out.printf("정적 게임 데이터 %d개 카드가 메모리에 로드되었습니다.%n", allCards.size());


        this.allCards = Collections.unmodifiableList(this.allCards);
        this.allNobles = Collections.unmodifiableList(this.allNobles);
    }

    public List<StaticCard> getShuffledLevelCards(int level) {

        List<StaticCard> filteredCards = allCards.stream()
                .filter(card -> card.level() == level)
                .collect(Collectors.toList());

        Collections.shuffle(filteredCards);


        return Collections.unmodifiableList(filteredCards);
    }

    // 모든 귀족 타일을 조회하는 메서드
    public List<StaticNoble> getAllNobles() {
        return allNobles;
    }


    private List<StaticCard> createCardList() {
        return List.of(
                // --- 1단계 카드 (DIAMOND 보너스) ---
                new StaticCard(1, 1, 0, DIAMOND, 0, 1, 1, 1, 1),
                new StaticCard(2, 1, 0, DIAMOND, 0, 1, 2, 1, 1),
                new StaticCard(3, 1, 0, DIAMOND, 0, 1, 2, 2, 0),
                new StaticCard(4, 1, 0, DIAMOND, 0, 2, 0, 2, 1),
                new StaticCard(5, 1, 0, DIAMOND, 3, 0, 0, 0, 0),
                new StaticCard(6, 1, 1, DIAMOND, 0, 4, 0, 0, 0),
                new StaticCard(7, 1, 0, DIAMOND, 2, 1, 3, 0, 0),
                new StaticCard(8, 1, 0, DIAMOND, 2, 0, 0, 0, 2),
                // --- 1단계 카드 (SAPPHIRE 보너스) ---
                new StaticCard(9, 1, 0, SAPPHIRE, 1, 0, 1, 1, 1),
                new StaticCard(10, 1, 0, SAPPHIRE, 0, 0, 1, 1, 2),
                new StaticCard(11, 1, 0, SAPPHIRE, 0, 0, 2, 2, 1),
                new StaticCard(12, 1, 0, SAPPHIRE, 0, 0, 2, 1, 2),
                new StaticCard(13, 1, 0, SAPPHIRE, 0, 0, 3, 0, 0),
                new StaticCard(14, 1, 1, SAPPHIRE, 0, 0, 4, 0, 0),
                new StaticCard(15, 1, 0, SAPPHIRE, 0, 0, 2, 1, 3),
                new StaticCard(16, 1, 0, SAPPHIRE, 2, 0, 0, 2, 0),
                // --- 1단계 카드 (EMERALD 보너스) ---
                new StaticCard(17, 1, 0, EMERALD, 1, 1, 0, 1, 1),
                new StaticCard(18, 1, 0, EMERALD, 1, 1, 0, 1, 2),
                new StaticCard(19, 1, 0, EMERALD, 0, 2, 0, 1, 2),
                new StaticCard(20, 1, 0, EMERALD, 2, 1, 0, 2, 0),
                new StaticCard(21, 1, 0, EMERALD, 0, 3, 0, 0, 0),
                new StaticCard(22, 1, 1, EMERALD, 0, 0, 0, 4, 0),
                new StaticCard(23, 1, 0, EMERALD, 0, 2, 0, 3, 1),
                new StaticCard(24, 1, 0, EMERALD, 0, 2, 0, 0, 2),
                // --- 1단계 카드 (RUBY 보너스) ---
                new StaticCard(25, 1, 0, RUBY, 1, 1, 1, 0, 1),
                new StaticCard(26, 1, 0, RUBY, 1, 1, 2, 0, 1),
                new StaticCard(27, 1, 0, RUBY, 1, 2, 2, 0, 0),
                new StaticCard(28, 1, 0, RUBY, 0, 2, 1, 0, 2),
                new StaticCard(29, 1, 0, RUBY, 0, 0, 0, 0, 3),
                new StaticCard(30, 1, 1, RUBY, 4, 0, 0, 0, 0),
                new StaticCard(31, 1, 0, RUBY, 1, 2, 3, 0, 0),
                new StaticCard(32, 1, 0, RUBY, 0, 2, 2, 0, 0),
                // --- 1단계 카드 (ONYX 보너스) ---
                new StaticCard(33, 1, 0, ONYX, 1, 1, 1, 1, 0),
                new StaticCard(34, 1, 0, ONYX, 1, 2, 1, 1, 0),
                new StaticCard(35, 1, 0, ONYX, 2, 0, 2, 1, 0),
                new StaticCard(36, 1, 0, ONYX, 0, 2, 2, 1, 0),
                new StaticCard(37, 1, 0, ONYX, 0, 0, 0, 3, 0),
                new StaticCard(38, 1, 1, ONYX, 0, 0, 0, 4, 0),
                new StaticCard(39, 1, 0, ONYX, 3, 0, 0, 1, 2),
                new StaticCard(40, 1, 0, ONYX, 2, 0, 2, 0, 0),
                // --- 2단계 카드 ---
                new StaticCard(41, 2, 1, DIAMOND, 0, 2, 0, 3, 2),
                new StaticCard(42, 2, 2, DIAMOND, 5, 0, 0, 0, 0),
                new StaticCard(43, 2, 2, DIAMOND, 3, 0, 3, 2, 0),
                new StaticCard(44, 2, 2, DIAMOND, 0, 1, 4, 0, 2),
                new StaticCard(45, 2, 3, DIAMOND, 6, 0, 0, 0, 0),
                new StaticCard(46, 2, 1, DIAMOND, 3, 0, 5, 0, 0),
                new StaticCard(47, 2, 1, SAPPHIRE, 3, 0, 2, 2, 0),
                new StaticCard(48, 2, 2, SAPPHIRE, 0, 5, 0, 0, 0),
                new StaticCard(49, 2, 2, SAPPHIRE, 0, 0, 3, 2, 3),
                new StaticCard(50, 2, 2, SAPPHIRE, 0, 3, 0, 5, 0),
                new StaticCard(51, 2, 3, SAPPHIRE, 0, 6, 0, 0, 0),
                new StaticCard(52, 2, 1, SAPPHIRE, 0, 3, 0, 0, 5),
                new StaticCard(53, 2, 1, EMERALD, 2, 2, 0, 3, 0),
                new StaticCard(54, 2, 2, EMERALD, 0, 0, 5, 0, 0),
                new StaticCard(55, 2, 2, EMERALD, 2, 0, 3, 3, 0),
                new StaticCard(56, 2, 2, EMERALD, 1, 2, 0, 4, 0),
                new StaticCard(57, 2, 3, EMERALD, 0, 0, 6, 0, 0),
                new StaticCard(58, 2, 1, EMERALD, 0, 0, 3, 5, 0),
                new StaticCard(59, 2, 1, RUBY, 0, 2, 3, 0, 2),
                new StaticCard(60, 2, 2, RUBY, 0, 0, 0, 5, 0),
                new StaticCard(61, 2, 2, RUBY, 3, 2, 0, 0, 3),
                new StaticCard(62, 2, 2, RUBY, 2, 4, 0, 1, 0),
                new StaticCard(63, 2, 3, RUBY, 0, 0, 0, 6, 0),
                new StaticCard(64, 2, 1, RUBY, 0, 5, 0, 3, 0),
                new StaticCard(65, 2, 1, ONYX, 0, 0, 2, 2, 3),
                new StaticCard(66, 2, 2, ONYX, 0, 0, 0, 0, 5),
                new StaticCard(67, 2, 2, ONYX, 0, 3, 0, 3, 2),
                new StaticCard(68, 2, 2, ONYX, 0, 0, 1, 2, 4),
                new StaticCard(69, 2, 3, ONYX, 0, 0, 0, 0, 6),
                new StaticCard(70, 2, 1, ONYX, 0, 3, 0, 5, 0),
                // --- 3단계 카드 ---
                new StaticCard(71, 3, 3, DIAMOND, 0, 3, 5, 3, 3),
                new StaticCard(72, 3, 4, DIAMOND, 7, 0, 0, 0, 0),
                new StaticCard(73, 3, 4, DIAMOND, 0, 3, 3, 0, 6),
                new StaticCard(74, 3, 5, DIAMOND, 7, 0, 0, 0, 3),
                new StaticCard(75, 3, 3, SAPPHIRE, 3, 0, 3, 3, 5),
                new StaticCard(76, 3, 4, SAPPHIRE, 0, 7, 0, 0, 0),
                new StaticCard(77, 3, 4, SAPPHIRE, 0, 0, 6, 3, 3),
                new StaticCard(78, 3, 5, SAPPHIRE, 3, 7, 0, 0, 0),
                new StaticCard(79, 3, 3, EMERALD, 3, 3, 5, 0, 3),
                new StaticCard(80, 3, 4, EMERALD, 0, 0, 7, 0, 0),
                new StaticCard(81, 3, 4, EMERALD, 3, 6, 3, 0, 0),
                new StaticCard(82, 3, 5, EMERALD, 0, 0, 7, 3, 0),
                new StaticCard(83, 3, 3, RUBY, 3, 5, 3, 3, 0),
                new StaticCard(84, 3, 4, RUBY, 0, 0, 0, 7, 0),
                new StaticCard(85, 3, 4, RUBY, 0, 3, 0, 6, 3),
                new StaticCard(86, 3, 5, RUBY, 0, 0, 0, 7, 3),
                new StaticCard(87, 3, 3, ONYX, 3, 3, 3, 5, 0),
                new StaticCard(88, 3, 4, ONYX, 0, 0, 0, 0, 7),
                new StaticCard(89, 3, 4, ONYX, 0, 0, 3, 3, 6),
                new StaticCard(90, 3, 5, ONYX, 0, 0, 7, 0, 3)
        );
    }

    private List<StaticNoble> createNobleList() {
        return List.of(
                new StaticNoble(101, 3, 4, 0, 4, 0, 0),
                new StaticNoble(102, 3, 3, 3, 3, 0, 0),
                new StaticNoble(103, 3, 4, 0, 0, 4, 0),
                new StaticNoble(104, 3, 0, 0, 4, 0, 4),
                new StaticNoble(105, 3, 0, 3, 3, 0, 3),
                new StaticNoble(106, 3, 0, 3, 0, 3, 3),
                new StaticNoble(107, 3, 0, 0, 3, 3, 3),
                new StaticNoble(108, 3, 0, 0, 0, 4, 4),
                new StaticNoble(109, 3, 3, 3, 0, 3, 0),
                new StaticNoble(110, 3, 0, 4, 0, 4, 0)
        );
    }
}