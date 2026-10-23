package com.syncflow.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

/** Where each unfinished task goes when a sprint closes. Tasks not listed return to the backlog. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompleteSprintRequest {

    @Builder.Default
    private List<Move> moves = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Move {
        private Long taskId;
        /** Target sprint (must be in PLANNING), or null for the backlog. */
        private Long sprintId;
    }
}
