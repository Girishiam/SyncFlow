package com.syncflow.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyVerificationResponseDTO {
    private Boolean verified;
    private String elevatedApprovalToken;
    private String message;
    private String verifiedMethod;
    private Long expiresAtMillis;
}
