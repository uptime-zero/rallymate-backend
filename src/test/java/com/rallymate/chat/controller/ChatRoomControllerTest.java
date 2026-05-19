package com.rallymate.chat.controller;

import com.rallymate.chat.entity.ChatRoom;
import com.rallymate.chat.entity.ChatRoomStatus;
import com.rallymate.chat.service.ChatRoomService;
import com.rallymate.global.config.SecurityConfig;
import com.rallymate.global.exception.ErrorCode;
import com.rallymate.global.exception.NotFoundException;
import com.rallymate.global.jwt.JwtProvider;
import com.rallymate.user.entity.Gender;
import com.rallymate.user.entity.User;
import com.rallymate.user.entity.UserRole;
import com.rallymate.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatRoomController.class)
@Import(SecurityConfig.class)
class ChatRoomControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ChatRoomService chatRoomService;

    @MockitoBean
    JwtProvider jwtProvider;

    @MockitoBean
    UserRepository userRepository;

    private User participant;
    private User outsider;
    private ChatRoom chatRoom;

    @BeforeEach
    void setUp() {
        participant = User.of("uid-participant", "enc", "hash", "참여자", Gender.MALE, "url", "테니스", "서울", UserRole.USER);
        outsider = User.of("uid-outsider", "enc2", "hash2", "외부인", Gender.FEMALE, "url2", "배드민턴", "부산", UserRole.USER);
        chatRoom = ChatRoom.of("test-ulid", 1L, "uid-participant", "uid-other", LocalDateTime.now());
    }

    private UsernamePasswordAuthenticationToken authOf(User user) {
        return new UsernamePasswordAuthenticationToken(user, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    // ── getChatRoom ───────────────────────────────────────────────────────────

    @Test
    void getChatRoom_성공() throws Exception {
        given(chatRoomService.getByUlid("test-ulid")).willReturn(chatRoom);

        mockMvc.perform(get("/api/v1/chat-rooms/{ulid}", "test-ulid")
                        .with(authentication(authOf(participant))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.ulid").value("test-ulid"))
                .andExpect(jsonPath("$.data.status").value(ChatRoomStatus.ACTIVE.name()));
    }

    @Test
    void getChatRoom_방_없음_실패() throws Exception {
        given(chatRoomService.getByUlid("none")).willThrow(new NotFoundException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        mockMvc.perform(get("/api/v1/chat-rooms/{ulid}", "none")
                        .with(authentication(authOf(participant))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.CHAT_ROOM_NOT_FOUND.getCode()));
    }

    @Test
    void getChatRoom_참여자_아님_실패() throws Exception {
        given(chatRoomService.getByUlid("test-ulid")).willReturn(chatRoom);

        mockMvc.perform(get("/api/v1/chat-rooms/{ulid}", "test-ulid")
                        .with(authentication(authOf(outsider))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_A_CHAT_ROOM_PARTICIPANT.getCode()));
    }
}
