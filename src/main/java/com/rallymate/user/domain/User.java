package com.rallymate.user.domain;

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

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String phoneNumberHash;

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

    @Builder(access = AccessLevel.PRIVATE)
    private User(
            String phoneNumber,
            String phoneNumberHash,
            String nickname,
            Gender gender,
            String profileUrl,
            String preferredSport,
            String activityArea
    ) {
        this.phoneNumber = phoneNumber;
        this.phoneNumberHash = phoneNumberHash;
        this.nickname = nickname;
        this.gender = gender;
        this.profileUrl = profileUrl;
        this.preferredSport = preferredSport;
        this.activityArea = activityArea;
    }

    public static User of(
            String phoneNumber,
            String phoneNumberHash,
            String nickname,
            Gender gender,
            String profileUrl,
            String preferredSport,
            String activityArea
    ) {
        return User.builder()
                .phoneNumber(phoneNumber)
                .phoneNumberHash(phoneNumberHash)
                .nickname(nickname)
                .gender(gender)
                .profileUrl(profileUrl)
                .preferredSport(preferredSport)
                .activityArea(activityArea)
                .build();
    }
}
