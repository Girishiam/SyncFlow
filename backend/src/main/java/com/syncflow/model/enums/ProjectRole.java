package com.syncflow.model.enums;

/** A person's role inside one project, independent of their organisation-wide role. */
public enum ProjectRole {
    /** Owns the project: manages the team and settings. */
    MANAGER,
    /** Runs the day-to-day work: can also manage the team. */
    LEAD,
    MEMBER;

    public boolean canManageTeam() {
        return this == MANAGER || this == LEAD;
    }
}
