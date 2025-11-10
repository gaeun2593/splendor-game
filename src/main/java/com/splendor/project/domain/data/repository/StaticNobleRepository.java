package com.splendor.project.domain.data.repository;

import com.splendor.project.domain.data.entity.StaticNoble;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StaticNobleRepository extends JpaRepository<StaticNoble, Long> {
}
