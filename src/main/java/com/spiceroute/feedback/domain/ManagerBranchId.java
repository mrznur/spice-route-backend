package com.spiceroute.feedback.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class ManagerBranchId implements Serializable {

    private UUID userId;
    private UUID branchId;

    public ManagerBranchId() {}

    public ManagerBranchId(UUID userId, UUID branchId) {
        this.userId = userId;
        this.branchId = branchId;
    }

    public UUID getUserId() { return userId; }
    public UUID getBranchId() { return branchId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ManagerBranchId other)) return false;
        return Objects.equals(userId, other.userId) && Objects.equals(branchId, other.branchId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, branchId);
    }
}
