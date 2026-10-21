package com.syncflow.dto;

import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateDeadlineRequest {
    private Instant deadline;
}
