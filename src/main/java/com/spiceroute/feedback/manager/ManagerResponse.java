package com.spiceroute.feedback.manager;

import com.spiceroute.feedback.domain.Branch;
import com.spiceroute.feedback.domain.User;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ManagerResponse(
        UUID id,
        String name,
        String email,
        boolean active,
        Instant createdAt,
        List<BranchInfo> branches) {

    /** Slim branch projection so the frontend doesn't need a second call to show branch names. */
    public record BranchInfo(UUID id, String name) {
        public static BranchInfo from(Branch b) {
            return new BranchInfo(b.getId(), b.getName());
        }
    }

    public static ManagerResponse from(User u, List<BranchInfo> branches) {
        return new ManagerResponse(u.getId(), u.getName(), u.getEmail(),
                u.isActive(), u.getCreatedAt(), branches);
    }
}
