package com.rallymate.chat.controller;

import com.rallymate.chat.dto.ChatRoomResponse;
import com.rallymate.chat.entity.ChatRoom;
import com.rallymate.chat.service.ChatRoomService;
import com.rallymate.global.response.ApiResponse;
import com.rallymate.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat-rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @GetMapping("/{ulid}")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> getChatRoom(
            @PathVariable String ulid,
            @AuthenticationPrincipal User user
    ) {
        ChatRoom room = chatRoomService.getByUlid(ulid);
        room.requireParticipant(user.getUid());
        return ResponseEntity.ok(ApiResponse.ok(ChatRoomResponse.from(room)));
    }
}
