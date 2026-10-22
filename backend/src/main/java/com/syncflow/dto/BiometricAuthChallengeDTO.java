package com.syncflow.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BiometricAuthChallengeDTO {
    private String challengeId;
    private String challengeToken;
    private String rpName;
    private String rpId;
    private String userEmail;
    private Long expiresAtMillis;
}
