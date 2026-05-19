package com.rallymate.matching.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MatchSessionFinishedEvent {

    private Long sessionId;
    private String uidA;
    private String uidB;
    private boolean cancelled;
}
