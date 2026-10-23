package com.syncflow.model;

import com.syncflow.model.enums.*;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "sprints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sprint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String goal;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(nullable = false, length = 20)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private SprintStatus status = SprintStatus.PLANNING;

    @Column(name = "target_story_points")
    @Builder.Default
    private Integer targetStoryPoints = 30;

    @Column(name = "completed_at")
    private Instant completedAt;

    /** Story points finished when the sprint was closed; used for velocity. */
    @Column(name = "completed_story_points")
    private Integer completedStoryPoints;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
