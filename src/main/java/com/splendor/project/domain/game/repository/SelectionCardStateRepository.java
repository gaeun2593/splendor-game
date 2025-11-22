package com.splendor.project.domain.game.repository;

import com.splendor.project.domain.game.dto.response.SelectionCardStateDto;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SelectionCardStateRepository extends CrudRepository<SelectionCardStateDto, Long> {
    Optional<SelectionCardStateDto> findById(Long roomId);
}