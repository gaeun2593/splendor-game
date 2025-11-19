package com.splendor.project.domain.game.conreoller;

import com.splendor.project.domain.game.dto.response.GameStateDto;
import com.splendor.project.domain.game.request.ChoicePlayerDto;
import com.splendor.project.domain.game.service.PlayGameService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import static com.splendor.project.contants.StompConstants.TOPIC_ROOM_SPECIFIC_PREFIX;

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
    public void gameChoiceMessage(@Payload ChoicePlayerDto choicePlayerDto , @DestinationVariable Long roomId){
        System.out.println("choicePlayerDto = " + choicePlayerDto);
        String specificRoomTopic = "/topic/game-choice-screen/" + roomId;
        messagingTemplate.convertAndSend(specificRoomTopic ,choicePlayerDto.getSplendorAction());
    }


}
