package com.rallymate.matching.dto;

import com.rallymate.matching.entity.MatchResult;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MatchCompletionsRequest {

    @NotNull(message = "매칭 세션 ID는 필수입니다.")
    private Long matchSessionId;

    @NotNull(message = "매칭 결과는 필수입니다.")
    private MatchResult result;
}

