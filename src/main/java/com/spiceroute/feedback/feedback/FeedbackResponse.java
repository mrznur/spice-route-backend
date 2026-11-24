package com.spiceroute.feedback.feedback;

import com.spiceroute.feedback.domain.Feedback;

import java.time.Instant;
import java.util.UUID;

public record FeedbackResponse(
        UUID id,
        UUID branchId,
        UUID tableId,
        short rating,
        String comment,
        String customerName,
        boolean anonymous,
        String language,
        Instant createdAt
) {
    public static FeedbackResponse from(Feedback f) {
        return new FeedbackResponse(
                f.getId(),
                f.getBranchId(),
                f.getTableId(),
                f.getRating(),
                f.getComment(),
                f.isAnonymous() ? null : f.getCustomerName(),
                f.isAnonymous(),
                f.getLanguage(),
                f.getCreatedAt()
        );
    }
}
