package com.rallymate.matching.repository;

import com.rallymate.matching.entity.MatchSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchSessionRepository extends JpaRepository<MatchSession, Long> {
}

