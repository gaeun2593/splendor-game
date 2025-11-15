package com.splendor.project.domain.room.entity;


import com.splendor.project.domain.player.entity.Player;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roomId;

    @OneToMany(mappedBy = "room")
    private List<Player> players = new ArrayList<>() ;

    private String roomName ;

    @Enumerated(EnumType.STRING)
    private RoomStatus roomStatus ;

    public Room(String roomName, RoomStatus roomStatus) {
        this.roomName = roomName;
        this.roomStatus = roomStatus;
    }

}
