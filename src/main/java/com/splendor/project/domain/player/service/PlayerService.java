package com.splendor.project.domain.player.service;

import com.splendor.project.domain.player.dto.PlayerDto;
import com.splendor.project.domain.player.entity.Player;
import com.splendor.project.domain.player.repository.PlayerRepository;
import com.splendor.project.domain.room.dto.request.RequestRoomDto;
import com.splendor.project.domain.room.entity.Room;
import com.splendor.project.domain.room.entity.RoomStatus;
import com.splendor.project.domain.room.repository.RoomRepository;
import com.splendor.project.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final RoomRepository roomRepository ;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 닉네임 기반 간편 가입
     */

//    public PlayerDto.JoinResponseDto join(PlayerDto.JoinRequestDto requestDto) {
//
//        // 1. 닉네임 중복 검사
//        playerRepository.findByNickname(requestDto.getNickname())
//                .ifPresent(user -> {
//                    throw new IllegalArgumentException("이미 사용중인 닉네임입니다.");
//                });
////
////        // 2. 유저 생성 및 RDB에 저장
////       // Player newPlayer  = new Player(requestDto.getNickname());
////        playerRepository.save(newPlayer);
////
////        // 3. JWT 토큰 생성
////        String jwtToken = jwtTokenProvider.createToken(newPlayer.getPlayerId(), newPlayer.getNickname());
////
////        // 4. DTO로 변환하여 응답
////        return new PlayerDto.JoinResponseDto(newPlayer.getPlayerId(), newPlayer.getNickname(), jwtToken);
//    }

    @Transactional(readOnly = true)
    public List<Player> findAll(){
        return playerRepository.findAll();
    }

    public Player save(RequestRoomDto requestRoomDto){
        String roomName = requestRoomDto.getRoomName();
        Room room = new Room(roomName , RoomStatus.WAITING);
        Player player = new Player(room , requestRoomDto.getHostName() , true);// 처음 방을 만든사람이 방장
        return playerRepository.save(player);
    }

    public Player join(RequestRoomDto requestRoomDto, Long roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new NoSuchElementException("방이 존재하지 않습니다"));

        Player player = new Player(room, requestRoomDto.getHostName(), false);
        return playerRepository.save(player);
    }
}
