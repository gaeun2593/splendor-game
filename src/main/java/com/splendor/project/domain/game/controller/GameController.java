package com.splendor.project.domain.game.controller;

import com.splendor.project.domain.data.GemType;
import com.splendor.project.domain.game.dto.request.DiscardTokenRequestDto;
import com.splendor.project.domain.game.dto.request.SelectCardRequestDto;
import com.splendor.project.domain.game.dto.request.SelectTokenRequestDto;
import com.splendor.project.domain.game.dto.response.GameStateDto;
import com.splendor.project.domain.game.dto.response.SelectionCardStateDto;
import com.splendor.project.domain.game.dto.response.WebSocketResponse;
import com.splendor.project.domain.game.dto.request.ChoicePlayerDto;
import com.splendor.project.domain.game.service.PlayGameService;
import com.splendor.project.exception.GameLogicException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.NoSuchElementException;

@Controller
@RequiredArgsConstructor
public class GameController {

    private final SimpMessagingTemplate messagingTemplate;
    private final PlayGameService playGameService;

    @MessageMapping("/game-screen/{roomId}")
    public void gameStartMessage(@DestinationVariable Long roomId) {
        String specificRoomTopic = "/topic/game-screen/" + roomId;
        try {
            GameStateDto gameStateDto = playGameService.gameStart(roomId);
            messagingTemplate.convertAndSend(specificRoomTopic, WebSocketResponse.success(gameStateDto));
        } catch (NoSuchElementException | IllegalStateException e) {
            messagingTemplate.convertAndSend(specificRoomTopic, WebSocketResponse.error(e.getMessage()));
        }
    }

    @MessageMapping("/game-choice-screen/{roomId}")
    public void gameChoiceMessage(@Payload ChoicePlayerDto choicePlayerDto, @DestinationVariable Long roomId) {
        System.out.println("choicePlayerDto = " + choicePlayerDto);
        String specificRoomTopic = "/topic/game-choice-screen/" + roomId;
        messagingTemplate.convertAndSend(specificRoomTopic, choicePlayerDto.getSplendorAction());
    }

    // --- 토큰 선택/취소 (selectToken) ---
    @MessageMapping("/game-select-token/{roomId}")
    public void selectTokenMessage(@Payload SelectTokenRequestDto request, @DestinationVariable Long roomId) {
        String specificRoomTopic = "/topic/game-select-token/" + roomId;
        try {
            Map<GemType, Integer> selectedTokens = playGameService.selectToken(request);

            messagingTemplate.convertAndSend(specificRoomTopic, WebSocketResponse.success(selectedTokens));
        } catch (IllegalArgumentException | IllegalStateException | NoSuchElementException e) {
            messagingTemplate.convertAndSend(specificRoomTopic, WebSocketResponse.error(e.getMessage()));
        }
    }

    // --- 토큰 버리기 (discardToken) ---
    @MessageMapping("/game-discard-token/{roomId}")
    public void discardTokenMessage(@Payload DiscardTokenRequestDto request, @DestinationVariable Long roomId) {
        String gameScreenTopic = "/topic/game-screen/" + roomId;
        try {
            GameStateDto gameStateDto = playGameService.discardToken(request);

            messagingTemplate.convertAndSend(gameScreenTopic, WebSocketResponse.success(gameStateDto));
        } catch (IllegalArgumentException | IllegalStateException | NoSuchElementException e) {
            messagingTemplate.convertAndSend(gameScreenTopic, WebSocketResponse.error(e.getMessage()));
        }
    }

    // --- 카드 선택/취소 (selectCard) ---
    @MessageMapping("/game-select-card/{roomId}")
    public void selectCardMessage(@Payload SelectCardRequestDto request, @DestinationVariable Long roomId) {
        String specificRoomTopic = "/topic/game-select-card/" + roomId;
        try {
            SelectionCardStateDto selectionState = playGameService.selectCard(request);

            messagingTemplate.convertAndSend(specificRoomTopic, WebSocketResponse.success(selectionState));

        } catch (GameLogicException e) {
            messagingTemplate.convertAndSend(specificRoomTopic, WebSocketResponse.error(e.getMessage()));

        } catch (NoSuchElementException | IllegalStateException e) {
            messagingTemplate.convertAndSend(specificRoomTopic, WebSocketResponse.error(e.getMessage()));
        }
    }

    // --- 턴 종료 (endTurn) ---
    @MessageMapping("/game-end-turn/{roomId}")
    public void endTurnMessage(@DestinationVariable Long roomId) {
        String gameScreenTopic = "/topic/game-screen/" + roomId;
        try {
            GameStateDto gameStateDto = playGameService.endTurn(roomId);

            messagingTemplate.convertAndSend(gameScreenTopic, WebSocketResponse.success(gameStateDto));
        } catch (IllegalArgumentException | IllegalStateException | NoSuchElementException e) {
            messagingTemplate.convertAndSend(gameScreenTopic, WebSocketResponse.error(e.getMessage()));
        }
    }
}