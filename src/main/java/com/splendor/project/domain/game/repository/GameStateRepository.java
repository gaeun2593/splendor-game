package com.splendor.project.domain.game.repository;

import com.splendor.project.domain.game.dto.response.GameStateDto;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface GameStateRepository extends CrudRepository<GameStateDto, Long> {
    Optional<GameStateDto> findById(Long gameId);
}
