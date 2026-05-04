package com.rallymate.matching.repository;

import com.rallymate.matching.entity.MatchParticipantResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchParticipantResultRepository extends JpaRepository<MatchParticipantResult, Long> {

    List<MatchParticipantResult> findByMatchSessionId(Long matchSessionId);
}

