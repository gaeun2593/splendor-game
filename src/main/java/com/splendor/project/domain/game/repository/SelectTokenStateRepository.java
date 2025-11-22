package com.splendor.project.domain.game.repository;

import com.splendor.project.domain.game.dto.response.SelectTokenStateDto;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SelectTokenStateRepository extends CrudRepository<SelectTokenStateDto, Long> {
    Optional<SelectTokenStateDto> findById(Long roomId);
}
