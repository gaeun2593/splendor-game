package com.splendor.project.domain.game.dto.response;
import com.splendor.project.domain.game.dto.request.PlayerAction;
import lombok.Data;

@Data
public class SelectedPlayer {
    private String splendorAction;
    private PlayerAction playerAction;

    public SelectedPlayer(String splendorAction) {
        this.splendorAction = splendorAction;
        this.playerAction = PlayerAction.fromMessage(splendorAction);
    }
}
