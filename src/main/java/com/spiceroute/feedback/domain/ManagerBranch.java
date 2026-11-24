package com.spiceroute.feedback.domain;

import jakarta.persistence.*;
import java.util.UUID;

/** Assigns a manager (user) to a branch. */
@Entity
@Table(name = "manager_branches")
@IdClass(ManagerBranchId.class)
public class ManagerBranch {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "branch_id")
    private UUID branchId;

    protected ManagerBranch() {}

    public ManagerBranch(UUID userId, UUID branchId) {
        this.userId = userId;
        this.branchId = branchId;
    }

    public UUID getUserId() { return userId; }
    public UUID getBranchId() { return branchId; }
}
