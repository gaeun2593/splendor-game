package com.splendor.project.domain.game.controller;

import com.splendor.project.domain.data.GemType;
import com.splendor.project.domain.game.dto.request.DiscardTokenRequestDto;
import com.splendor.project.domain.game.dto.request.SelectTokenRequestDto;
import com.splendor.project.domain.game.dto.response.GameStateDto;
import com.splendor.project.domain.game.dto.request.ChoicePlayerDto;
import com.splendor.project.domain.game.dto.response.SelectedPlayer;
import com.splendor.project.domain.game.service.PlayGameService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class GameController {

    private final SimpMessagingTemplate messagingTemplate;
    private final PlayGameService playGameService;

    @MessageMapping("/game-screen/{roomId}")
    public void gameStartMessage(@DestinationVariable Long roomId){
        GameStateDto gameStateDto = playGameService.gameStart(roomId);
        String specificRoomTopic = "/topic/game-screen/" + roomId;
        messagingTemplate.convertAndSend(specificRoomTopic , gameStateDto);
    }

    @MessageMapping("/game-choice-screen/{roomId}")
    public void gameChoiceMessage(@Payload ChoicePlayerDto choicePlayerDto, @DestinationVariable Long roomId){
        String specificRoomTopic = "/topic/game-choice-screen/" + roomId;
        SelectedPlayer selectedPlayer = new SelectedPlayer(choicePlayerDto.getSplendorAction());
        messagingTemplate.convertAndSend(specificRoomTopic ,selectedPlayer);
    }

    @MessageMapping("/game-select-token/{roomId}")
    public void selectTokenMessage(@Payload SelectTokenRequestDto request, @DestinationVariable Long roomId) {
        // 1. 토큰 선택 로직 실행 (중간 검증 및 상태 저장)
        Map<GemType, Integer> selectedTokens = playGameService.selectToken(request);

        // 2. 현재까지 선택된 토큰 목록을 클라이언트에 전송하여 UI 업데이트 유도
        String specificRoomTopic = "/topic/game-select-token/" + roomId;
        messagingTemplate.convertAndSend(specificRoomTopic, selectedTokens);
    }

    @MessageMapping("/game-discard-token/{roomId}")
    public void discardTokenMessage(@Payload DiscardTokenRequestDto request, @DestinationVariable Long roomId){
        // 1. 서비스에서 토큰 버리기 로직 처리 및 최종 GameStateDto 업데이트
        GameStateDto gameStateDto = playGameService.discardToken(request);

        // 2. 버려진 후의 최종 게임 상태를 모든 플레이어에게 브로드캐스트
        String gameScreenTopic = "/topic/game-screen/" + roomId;
        messagingTemplate.convertAndSend(gameScreenTopic , gameStateDto);
    }

    @MessageMapping("/game-end-turn/{roomId}")
    public void endTurnMessage(@DestinationVariable Long roomId){
        // 1. 서비스에서 턴 종료 및 다음 플레이어 업데이트 로직 처리
        GameStateDto gameStateDto = playGameService.endTurn(roomId);

        // 2. 업데이트된 게임 상태를 모든 플레이어에게 브로드캐스트 (턴이 넘어갔음을 알림)
        String gameScreenTopic = "/topic/game-screen/" + roomId;
        messagingTemplate.convertAndSend(gameScreenTopic , gameStateDto);
    }
}
