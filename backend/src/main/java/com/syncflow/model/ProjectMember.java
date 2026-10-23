package com.syncflow.model;

import com.syncflow.model.enums.ProjectRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** Membership of a user in a project. Only members (and admins) can see the project or be assigned its tasks. */
@Entity
@Table(name = "project_members",
        uniqueConstraints = @UniqueConstraint(name = "UQ_project_members_project_user", columnNames = {"project_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ProjectRole role = ProjectRole.MEMBER;

    @Column(name = "added_at", nullable = false)
    @Builder.Default
    private Instant addedAt = Instant.now();
}
