package com.rallymate.chat.service;

import com.rallymate.chat.dto.ChatMessageResponse;
import com.rallymate.chat.entity.ChatMessage;
import com.rallymate.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private static final int DEFAULT_PAGE_SIZE = 50;

    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public ChatMessage saveText(Long chatRoomId, String senderUid, String content) {
        return chatMessageRepository.save(
                ChatMessage.text(chatRoomId, senderUid, content, LocalDateTime.now()));
    }

    @Transactional
    public ChatMessage saveSystem(Long chatRoomId, String content) {
        return chatMessageRepository.save(
                ChatMessage.system(chatRoomId, content, LocalDateTime.now()));
    }

    public List<ChatMessageResponse> listHistory(Long chatRoomId, Long cursorId, int size) {
        int pageSize = size > 0 ? size : DEFAULT_PAGE_SIZE;
        List<ChatMessage> messages;

        if (cursorId == null) {
            messages = chatMessageRepository.findByChatRoomIdOrderByCreatedAtAsc(
                    chatRoomId, PageRequest.of(0, pageSize));
        } else {
            messages = chatMessageRepository.findByChatRoomIdAndIdGreaterThanOrderByCreatedAtAsc(
                    chatRoomId, cursorId, PageRequest.of(0, pageSize));
        }

        return messages.stream().map(ChatMessageResponse::from).toList();
    }
}
