package com.syncflow.model;

import com.syncflow.model.enums.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "channels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id") // null for global channel like #general
    private Project project;

    @Column(nullable = false, length = 80)
    private String name; // general, api-discussion, bugs

    @Column(length = 255)
    private String description;

    @Column(length = 50)
    @Builder.Default
    private String icon = "hash";

    @Column(name = "is_private", nullable = false)
    @Builder.Default
    private Boolean isPrivate = false;

    @Column(nullable = false, length = 20)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private ChannelType type = ChannelType.TEXT;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "channel_members",
        joinColumns = @JoinColumn(name = "channel_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    @JsonIgnore
    private Set<User> members = new HashSet<>();

    @Transient
    public Integer getMemberCount() {
        try {
            return (members != null && org.hibernate.Hibernate.isInitialized(members)) ? members.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @Transient
    public String getCreatedByName() {
        return createdBy != null ? createdBy.getFullName() : null;
    }

    @Transient
    public Long getProjectId() {
        return project != null ? project.getId() : null;
    }

    @Transient
    public String getProjectName() {
        return project != null ? project.getName() : null;
    }
}
