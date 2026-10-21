package com.syncflow.controller;

import com.syncflow.dto.ChatMessageDTO;
import com.syncflow.dto.SendMessageRequest;
import com.syncflow.model.Channel;
import com.syncflow.model.User;
import com.syncflow.repository.UserRepository;
import com.syncflow.security.UserDetailsImpl;
import com.syncflow.service.ChannelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/channels")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class ChannelController {

    private final ChannelService channelService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<Channel>> getAllChannels() {
        return ResponseEntity.ok(channelService.getAllChannels());
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<ChatMessageDTO>> getChannelMessages(@PathVariable Long id) {
        return ResponseEntity.ok(channelService.getChannelMessages(id));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ChatMessageDTO> sendMessage(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody SendMessageRequest request) {

        UUID userId = userDetails != null ? userDetails.getId() : null;
        if (userId == null) {
            userId = userRepository.findAll().stream().findFirst().map(User::getId).orElse(null);
        }

        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }

        ChatMessageDTO message = channelService.postMessage(id, userId, request.getContent());
        messagingTemplate.convertAndSend("/topic/channel." + id, message);
        return ResponseEntity.ok(message);
    }
}
