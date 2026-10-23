package com.syncflow.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    /** Short-lived access token sent as {@code Authorization: Bearer}. */
    private String token;
    @Builder.Default
    private String type = "Bearer";
    /** Seconds until {@link #token} expires. */
    private long expiresIn;
    /** Single-use token exchanged at {@code /v1/auth/refresh} for a new token pair. */
    private String refreshToken;
    private UserDTO user;
}
