package com.splendor.project.domain.player.repository;

import com.splendor.project.domain.player.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    Optional<Player> findByNickname(String nickname); // 중복 체크

    @Override
    List<Player> findAll();


    @Override
    <S extends Player> S save(S entity);
}
