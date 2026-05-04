package com.rallymate.matching.controller;

import com.rallymate.matching.dto.MatchCompletionsRequest;
import com.rallymate.matching.dto.MatchResponsesRequest;
import com.rallymate.matching.dto.MatchStartRequest;
import com.rallymate.matching.service.MatchingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * 매칭 관련 STOMP 메시지를 처리하는 컨트롤러입니다.
 * <p>
 * - {@code /pub/api/v1/match/requests}: 매칭 시작
 * - {@code /pub/api/v1/match/responses}: 매칭 수락/거절 응답
 * - {@code /pub/api/v1/match/completions}: 매칭 종료 및 결과 제출
 * </p>
 */
@Controller
@MessageMapping("/api/v1/match")
@RequiredArgsConstructor
public class MatchingStompController {

    private final MatchingService matchingService;

    /**
     * 매칭을 시작합니다.
     *
     * @param principal 인증된 사용자 정보 (UID)
     * @param request   매칭 시작 정보 (종목, 위도, 경도)
     */
    @MessageMapping("/requests")
    public void requestsMatching(Principal principal, @Valid MatchStartRequest request) {
        matchingService.startMatching(principal.getName(), request);
    }

    /**
     * 매칭에 대한 수락/거절 응답을 처리합니다.
     *
     * @param principal 인증된 사용자 정보 (UID)
     * @param request   응답 정보 (매칭 세션 ID, 수락 여부)
     */
    @MessageMapping("/responses")
    public void responsesMatch(Principal principal, @Valid MatchResponsesRequest request) {
        matchingService.responsesMatch(principal.getName(), request);
    }

    /**
     * 매칭 종료 및 결과를 제출합니다.
     *
     * @param principal 인증된 사용자 정보 (UID)
     * @param request   종료 정보 (매칭 세션 ID, 결과)
     */
    @MessageMapping("/completions")
    public void completionsMatch(Principal principal, @Valid MatchCompletionsRequest request) {
        matchingService.completionsMatch(principal.getName(), request);
    }
}

