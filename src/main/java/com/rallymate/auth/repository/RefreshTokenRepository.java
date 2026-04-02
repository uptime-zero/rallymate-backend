package com.rallymate.auth.repository;

import com.rallymate.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByUid(String uid);

    boolean existsByToken(String token);

    void deleteByToken(String token);

    void deleteByUid(String uid);
}
