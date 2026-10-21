package com.syncflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReorderStagesRequest {
    @NotNull
    private List<Long> stageIds;
}
