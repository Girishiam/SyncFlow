package com.syncflow.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignTaskRequest {
    private UUID assigneeId;
}
