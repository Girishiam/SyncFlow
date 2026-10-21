package com.syncflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateChannelRequest {
    @NotBlank(message = "Channel name is required")
    @Size(min = 2, max = 80, message = "Channel name must be between 2 and 80 characters")
    private String name;

    private String description;

    @Builder.Default
    private String type = "TEXT"; // TEXT, VOICE, ANNOUNCEMENT

    @Builder.Default
    private String icon = "hash"; // hash, chat-dots, shield-lock, bank, code-slash, megaphone, cpu, terminal

    @Builder.Default
    private Boolean isPrivate = false;

    private Long projectId;

    private List<UUID> memberIds;
}
