package com.syncflow.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitLinkRequest {
    private String gitBranch;
    private String gitCommitHash;
    private String gitPrUrl;
}
