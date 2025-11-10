package com.splendor.project.domain.data.repository;

import com.splendor.project.domain.data.entity.StaticCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StaticCardRepository extends JpaRepository<StaticCard, Long> {
}
