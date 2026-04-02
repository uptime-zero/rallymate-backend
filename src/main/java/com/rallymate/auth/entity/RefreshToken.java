package com.rallymate.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String uid;

    @Column(nullable = false, unique = true)
    private String token;

    private boolean revoked;

    private LocalDateTime expiredAt;

    private LocalDateTime createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private RefreshToken(String uid, String token, boolean revoked, LocalDateTime expiredAt, LocalDateTime createdAt) {
        this.uid = uid;
        this.token = token;
        this.revoked = revoked;
        this.expiredAt = expiredAt;
        this.createdAt = createdAt;
    }

    public static RefreshToken of(String uid, String token, boolean revoked, LocalDateTime expiredAt, LocalDateTime createdAt) {
        return RefreshToken.builder()
                .uid(uid)
                .token(token)
                .revoked(revoked)
                .expiredAt(expiredAt)
                .createdAt(createdAt)
                .build();
    }

    public void revoke() {
        this.revoked = true;
    }
}
