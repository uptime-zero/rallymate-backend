package com.rallymate.chat.controller;

import com.rallymate.chat.dto.ChatMessageResponse;
import com.rallymate.chat.entity.ChatRoom;
import com.rallymate.chat.service.ChatMessageService;
import com.rallymate.chat.service.ChatRoomService;
import com.rallymate.global.response.ApiResponse;
import com.rallymate.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat-rooms")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    @GetMapping("/{ulid}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessages(
            @PathVariable String ulid,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal User user
    ) {
        ChatRoom room = chatRoomService.getByUlid(ulid);
        room.requireParticipant(user.getUid());

        // lazy reconciliation: ACTIVE이지만 세션이 이미 종료됐다면 LOCK 동기화
        if (room.getStatus().name().equals("ACTIVE")) {
            chatRoomService.requireActiveOrSync(ulid);
            room = chatRoomService.getByUlid(ulid);
        }

        List<ChatMessageResponse> messages = chatMessageService.listHistory(room.getId(), cursor, size);
        return ResponseEntity.ok(ApiResponse.ok(messages));
    }
}
