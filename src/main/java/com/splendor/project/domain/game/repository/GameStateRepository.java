package com.splendor.project.domain.game.repository;

import com.splendor.project.domain.game.dto.response.GameStateDto;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GameStateRepository extends CrudRepository<GameStateDto, Long> {
    Optional<GameStateDto> findById(Long gameId);
}
