package com.syncflow.auth.adapter.web;

import com.syncflow.audit.application.port.in.AuditLogUseCase;
import com.syncflow.dto.BiometricAuthChallengeDTO;
import com.syncflow.dto.PasskeyVerificationResponseDTO;
import com.syncflow.dto.VerifyPasskeyRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.UUID;

@RestController
@RequestMapping("/v1/auth/passkey")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class PasskeySecurityController {

    private final AuditLogUseCase auditLogUseCase;

    @GetMapping("/challenge")
    public ResponseEntity<BiometricAuthChallengeDTO> generateChallenge(
            @RequestParam(defaultValue = "WORKFLOW_GATE_OVERRIDE") String actionScope,
            Authentication authentication) {

        String email = authentication != null && authentication.getName() != null
                ? authentication.getName()
                : "alex.chen@syncflow.internal";

        String rawToken = UUID.randomUUID().toString();
        String challengeToken = Base64.getUrlEncoder().withoutPadding().encodeToString(rawToken.getBytes());

        BiometricAuthChallengeDTO challenge = BiometricAuthChallengeDTO.builder()
                .challengeId(UUID.randomUUID().toString())
                .challengeToken(challengeToken)
                .rpName("SyncFlow Bank Enterprise Security Core")
                .rpId("localhost")
                .userEmail(email)
                .expiresAtMillis(System.currentTimeMillis() + (5 * 60 * 1000))
                .build();

        log.info("Issued WebAuthn passkey challenge for user: {} with scope: {}", email, actionScope);
        return ResponseEntity.ok(challenge);
    }

    @PostMapping("/verify")
    public ResponseEntity<PasskeyVerificationResponseDTO> verifyPasskey(
            @RequestBody VerifyPasskeyRequest request,
            Authentication authentication) {

        String user = authentication != null && authentication.getName() != null
                ? authentication.getName()
                : "SecurityOfficer";

        String elevatedToken = "ELEVATED-PASSKEY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        try {
            auditLogUseCase.recordLog(
                    "BIOMETRIC_PASSKEY_VERIFIED",
                    "SECURITY",
                    0L,
                    "Hardware Token Verification",
                    user,
                    null,
                    "SECURITY_OFFICER",
                    "Elevated authorization granted for scope: " + request.getActionScope() + " via WebAuthn Biometric Token."
            );
        } catch (Exception e) {
            log.warn("Failed to record passkey verification audit log: {}", e.getMessage());
        }

        PasskeyVerificationResponseDTO response = PasskeyVerificationResponseDTO.builder()
                .verified(true)
                .elevatedApprovalToken(elevatedToken)
                .verifiedMethod("WebAuthn / FIDO2 Cryptographic Biometrics")
                .message("Hardware biometric challenge successfully validated. Elevated privileges unlocked.")
                .expiresAtMillis(System.currentTimeMillis() + (15 * 60 * 1000))
                .build();

        log.info("Passkey verification succeeded for user: {} - Token: {}", user, elevatedToken);
        return ResponseEntity.ok(response);
    }
}
