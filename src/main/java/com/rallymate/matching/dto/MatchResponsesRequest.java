package com.rallymate.matching.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MatchResponsesRequest {

    @NotNull(message = "매칭 세션 ID는 필수입니다.")
    private Long matchSessionId;

    @NotNull(message = "수락 여부는 필수입니다.")
    private Boolean accept;
}

