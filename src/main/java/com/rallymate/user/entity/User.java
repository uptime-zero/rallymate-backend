package com.rallymate.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 26)
    private String uid;

    @Column(nullable = false)
    private String phoneEnc;

    @Column(nullable = false, unique = true)
    private String phoneHash;

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(nullable = false)
    private String profileUrl;

    @Column(nullable = false)
    private String preferredSport;

    @Column(nullable = false)
    private String activityArea;

    @Column(nullable = false)
    private int eloRating = 1000;

    @Column(nullable = false)
    private double mannerScore = 36.5;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Builder(access = AccessLevel.PRIVATE)
    private User(
            String uid,
            String phoneEnc,
            String phoneHash,
            String nickname,
            Gender gender,
            String profileUrl,
            String preferredSport,
            String activityArea,
            UserRole role
    ) {
        this.uid = uid;
        this.phoneEnc = phoneEnc;
        this.phoneHash = phoneHash;
        this.nickname = nickname;
        this.gender = gender;
        this.profileUrl = profileUrl;
        this.preferredSport = preferredSport;
        this.activityArea = activityArea;
        this.role = role;
    }

    public static User of(
            String uid,
            String phoneEnc,
            String phoneHash,
            String nickname,
            Gender gender,
            String profileUrl,
            String preferredSport,
            String activityArea,
            UserRole role
    ) {
        return User.builder()
                .uid(uid)
                .phoneEnc(phoneEnc)
                .phoneHash(phoneHash)
                .nickname(nickname)
                .gender(gender)
                .profileUrl(profileUrl)
                .preferredSport(preferredSport)
                .activityArea(activityArea)
                .role(role)
                .build();
    }
}
