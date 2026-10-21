package com.syncflow.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectBoardDTO {
    private Long id;
    private String name;
    private String description;
    private String status;
    @Builder.Default
    private List<StageDTO> stages = new ArrayList<>();
    private long totalTasks;
    private long blockedTasks;
    private int completionPercentage;
}
