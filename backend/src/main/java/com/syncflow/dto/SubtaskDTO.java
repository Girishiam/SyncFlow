package com.syncflow.dto;

import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubtaskDTO {
    private Long id;
    private Long taskId;
    private String title;
    private Boolean isCompleted;
    private String completedBy;
    private Instant completedAt;
    private Integer position;
}
