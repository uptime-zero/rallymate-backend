package com.rallymate.chat.repository;

import com.rallymate.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId, Pageable pageable);

    List<ChatMessage> findByChatRoomIdAndIdGreaterThanOrderByCreatedAtAsc(
            Long chatRoomId, Long cursorId, Pageable pageable);
}
