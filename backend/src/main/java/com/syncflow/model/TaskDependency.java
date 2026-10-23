package com.syncflow.model;

import com.syncflow.model.enums.*;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "task_dependency_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_task_id", nullable = false)
    private Long sourceTaskId;

    @Column(name = "target_task_id", nullable = false)
    private Long targetTaskId;

    @Column(name = "target_task_title", length = 255)
    private String targetTaskTitle;

    @Column(name = "dependency_type", nullable = false, length = 30)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private DependencyType dependencyType = DependencyType.BLOCKS;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
