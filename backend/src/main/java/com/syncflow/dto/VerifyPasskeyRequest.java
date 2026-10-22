package com.syncflow.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyPasskeyRequest {
    private String challengeId;
    private String clientDataJSON;
    private String authenticatorData;
    private String signature;
    private String actionScope; // e.g. WORKFLOW_OVERRIDE, CRITICAL_APPROVAL, ROLE_CHANGE
}
