package com.syncflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWebhookRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String targetUrl;
    private String secretToken;
    private List<String> events;
}
