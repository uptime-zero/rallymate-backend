package com.rallymate.chat.service;

import com.github.f4b6a3.ulid.UlidCreator;
import com.rallymate.chat.entity.ChatRoom;
import com.rallymate.chat.entity.ChatRoomStatus;
import com.rallymate.chat.repository.ChatRoomRepository;
import com.rallymate.global.exception.ErrorCode;
import com.rallymate.global.exception.NotFoundException;
import com.rallymate.matching.entity.MatchSession;
import com.rallymate.matching.repository.MatchSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final MatchSessionRepository matchSessionRepository;

    @Transactional
    public ChatRoom createForMatch(Long sessionId, String uidA, String uidB) {
        if (chatRoomRepository.existsByMatchSessionId(sessionId)) {
            log.debug("ChatRoom already exists for sessionId={}, skipping", sessionId);
            return chatRoomRepository.findByMatchSessionId(sessionId).orElseThrow();
        }

        MatchSession session = matchSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.MATCHING_SESSION_NOT_FOUND));

        String ulid = UlidCreator.getUlid().toString();
        LocalDateTime now = LocalDateTime.now();

        ChatRoom room = ChatRoom.of(ulid, sessionId, uidA, uidB, now);
        chatRoomRepository.save(room);

        session.attachChatRoom(ulid);
        matchSessionRepository.save(session);

        log.debug("ChatRoom created ulid={} for sessionId={}", ulid, sessionId);
        return room;
    }

    @Transactional
    public void lockByMatchSessionId(Long sessionId) {
        chatRoomRepository.findByMatchSessionId(sessionId).ifPresent(room -> {
            room.lock(LocalDateTime.now());
            chatRoomRepository.save(room);
            log.debug("ChatRoom locked ulid={} for sessionId={}", room.getUlid(), sessionId);
        });
    }

    public Optional<ChatRoom> findByMatchSessionId(Long sessionId) {
        return chatRoomRepository.findByMatchSessionId(sessionId);
    }

    public ChatRoom getByUlid(String ulid) {
        return chatRoomRepository.findByUlid(ulid)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    /**
     * 채팅방이 ACTIVE인지 확인한다. Redis 메시지 유실 대비 lazy reconciliation 포함:
     * 채팅방이 아직 ACTIVE이지만 매칭 세션이 이미 종료됐다면 LOCKED로 동기화한다.
     */
    @Transactional
    public void requireActiveOrSync(String ulid) {
        ChatRoom room = getByUlid(ulid);

        if (room.getStatus() == ChatRoomStatus.LOCKED) {
            room.requireActive();
            return;
        }

        matchSessionRepository.findById(room.getMatchSessionId()).ifPresent(session -> {
            if (session.isFinished()) {
                room.lock(LocalDateTime.now());
                chatRoomRepository.save(room);
                log.debug("ChatRoom lazily locked ulid={} via session state", ulid);
            }
        });

        room.requireActive();
    }
}
