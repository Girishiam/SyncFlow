package com.syncflow.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permissions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"module", "action"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String module; // PROJECT, TASK, STAGE, CHAT, MEETING, ADMIN, ANALYTICS

    @Column(nullable = false, length = 50)
    private String action; // CREATE, VIEW, EDIT, DELETE, APPROVE, ASSIGN, HOST_MEETING

    @Column(length = 255)
    private String description;

    public String getAuthorityString() {
        return module.toUpperCase() + ":" + action.toUpperCase();
    }
}
