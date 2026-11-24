package com.spiceroute.feedback.table;

import com.spiceroute.feedback.domain.DiningTable;

import java.time.Instant;
import java.util.UUID;

public record TableResponse(UUID id, UUID branchId, String label, String qrToken, boolean active, Instant createdAt) {
    public static TableResponse from(DiningTable t) {
        return new TableResponse(t.getId(), t.getBranchId(), t.getLabel(), t.getQrToken(),
                t.isActive(), t.getCreatedAt());
    }
}
