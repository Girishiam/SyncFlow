package com.syncflow.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StageDTO {
    private Long id;
    private String name;
    private Integer position;
    private Boolean isDoneStage;
    @Builder.Default
    private List<TaskDTO> tasks = new ArrayList<>();
}
