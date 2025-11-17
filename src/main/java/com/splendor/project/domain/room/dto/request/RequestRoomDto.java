package com.splendor.project.domain.room.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RequestRoomDto {
    private String roomName;
    private String hostName;
    private boolean isHosted ;
}
