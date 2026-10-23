package com.syncflow.chat.adapter.web;

import com.syncflow.chat.application.port.in.ChatUseCase;
import com.syncflow.dto.*;
import com.syncflow.model.Channel;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChatUseCase chatUseCase;

    @GetMapping
    public ResponseEntity<List<Channel>> getAllChannels() {
        return ResponseEntity.ok(chatUseCase.getAllChannels());
    }

    @PostMapping
    @PreAuthorize("@access.can('CHAT:CREATE_CHANNEL') and (#request.projectId == null or @access.project(#request.projectId))")
    public ResponseEntity<Channel> createChannel(@Valid @RequestBody CreateChannelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatUseCase.createChannel(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@access.can('CHAT:EDIT_CHANNEL') and @access.channel(#id)")
    public ResponseEntity<Channel> updateChannel(@PathVariable Long id, @Valid @RequestBody UpdateChannelRequest request) {
        return ResponseEntity.ok(chatUseCase.updateChannel(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@access.can('CHAT:DELETE_CHANNEL') and @access.channel(#id)")
    public ResponseEntity<Void> deleteChannel(@PathVariable Long id) {
        chatUseCase.deleteChannel(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("@access.channel(#id)")
    public ResponseEntity<List<UserDTO>> getChannelMembers(@PathVariable Long id) {
        return ResponseEntity.ok(chatUseCase.getChannelMembers(id));
    }

    @PostMapping("/{id}/members/{memberId}")
    @PreAuthorize("@access.can('CHAT:MANAGE_MEMBERS') and @access.channel(#id)")
    public ResponseEntity<Void> addMember(@PathVariable Long id, @PathVariable UUID memberId) {
        chatUseCase.addMemberToChannel(id, memberId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @PreAuthorize("@access.can('CHAT:MANAGE_MEMBERS') and @access.channel(#id)")
    public ResponseEntity<Void> removeMember(@PathVariable Long id, @PathVariable UUID memberId) {
        chatUseCase.removeMemberFromChannel(id, memberId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Message history, oldest first. Pass {@code before} (a message id) to page further back.
     */
    @GetMapping("/{id}/messages")
    @PreAuthorize("@access.channel(#id)")
    public ResponseEntity<List<ChatMessageDTO>> getChannelMessages(
            @PathVariable Long id,
            @RequestParam(required = false) Long before,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(chatUseCase.getChannelMessages(id, before, limit));
    }

    @PostMapping("/{id}/messages")
    @PreAuthorize("@access.channel(#id)")
    public ResponseEntity<ChatMessageDTO> sendMessage(@PathVariable Long id, @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(chatUseCase.postMessage(id, request.getContent()));
    }
}
