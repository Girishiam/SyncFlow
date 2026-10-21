package com.syncflow.chat.adapter.web;

import com.syncflow.auth.application.port.out.UserRepositoryPort;
import com.syncflow.chat.application.port.in.ChatUseCase;
import com.syncflow.dto.ChatMessageDTO;
import com.syncflow.dto.SendMessageRequest;
import com.syncflow.model.Channel;
import com.syncflow.model.User;
import com.syncflow.security.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/channels")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class ChannelController {

    private final ChatUseCase chatUseCase;
    private final UserRepositoryPort userRepositoryPort;

    @GetMapping
    public ResponseEntity<List<Channel>> getAllChannels() {
        return ResponseEntity.ok(chatUseCase.getAllChannels());
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<ChatMessageDTO>> getChannelMessages(@PathVariable Long id) {
        return ResponseEntity.ok(chatUseCase.getChannelMessages(id));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ChatMessageDTO> sendMessage(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody SendMessageRequest request) {

        UUID userId = userDetails != null ? userDetails.getId() : null;
        if (userId == null) {
            userId = userRepositoryPort.findAll().stream().findFirst().map(User::getId).orElse(null);
        }

        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(chatUseCase.postMessage(id, userId, request.getContent()));
    }
}
