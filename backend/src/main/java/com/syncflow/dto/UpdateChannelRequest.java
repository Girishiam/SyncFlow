package com.syncflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateChannelRequest {
    @NotBlank(message = "Channel name is required")
    @Size(min = 2, max = 80, message = "Channel name must be between 2 and 80 characters")
    private String name;

    private String description;

    @Builder.Default
    private String type = "TEXT";

    @Builder.Default
    private String icon = "hash";

    private Boolean isPrivate;
}
