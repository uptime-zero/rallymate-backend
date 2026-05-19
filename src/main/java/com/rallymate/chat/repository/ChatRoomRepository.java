package com.rallymate.chat.repository;

import com.rallymate.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByUlid(String ulid);

    Optional<ChatRoom> findByMatchSessionId(Long matchSessionId);

    boolean existsByMatchSessionId(Long matchSessionId);
}
