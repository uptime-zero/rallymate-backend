package com.rallymate.matching.repository;

import com.rallymate.matching.entity.MatchingPenalty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MatchingPenaltyRepository extends JpaRepository<MatchingPenalty, Long> {

    Optional<MatchingPenalty> findByUid(String uid);
}

