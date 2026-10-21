package com.syncflow.chat.adapter.web;

import com.syncflow.auth.application.port.out.UserRepositoryPort;
import com.syncflow.chat.application.port.in.ChatUseCase;
import com.syncflow.dto.*;
import com.syncflow.model.Channel;
import com.syncflow.model.User;
import com.syncflow.security.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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

    @PostMapping
    public ResponseEntity<Channel> createChannel(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody CreateChannelRequest request) {

        UUID userId = getUserId(userDetails);
        Channel created = chatUseCase.createChannel(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Channel> updateChannel(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody UpdateChannelRequest request) {

        UUID userId = getUserId(userDetails);
        Channel updated = chatUseCase.updateChannel(id, request, userId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChannel(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        UUID userId = getUserId(userDetails);
        chatUseCase.deleteChannel(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<UserDTO>> getChannelMembers(@PathVariable Long id) {
        return ResponseEntity.ok(chatUseCase.getChannelMembers(id));
    }

    @PostMapping("/{id}/members/{memberId}")
    public ResponseEntity<Void> addMember(
            @PathVariable Long id,
            @PathVariable UUID memberId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        UUID requesterId = getUserId(userDetails);
        chatUseCase.addMemberToChannel(id, memberId, requesterId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long id,
            @PathVariable UUID memberId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        UUID requesterId = getUserId(userDetails);
        chatUseCase.removeMemberFromChannel(id, memberId, requesterId);
        return ResponseEntity.noContent().build();
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

        UUID userId = getUserId(userDetails);
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(chatUseCase.postMessage(id, userId, request.getContent()));
    }

    private UUID getUserId(UserDetailsImpl userDetails) {
        if (userDetails != null && userDetails.getId() != null) {
            return userDetails.getId();
        }
        return userRepositoryPort.findAll().stream().findFirst().map(User::getId).orElse(null);
    }
}

