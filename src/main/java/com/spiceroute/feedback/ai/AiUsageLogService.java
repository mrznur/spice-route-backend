
package com.spiceroute.feedback.ai;

import com.spiceroute.feedback.domain.AiUsageLog;
import com.spiceroute.feedback.repository.AiUsageLogRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AiUsageLogService {

    private final AiUsageLogRepository usageLogs;

    public AiUsageLogService(AiUsageLogRepository usageLogs) {
        this.usageLogs = usageLogs;
    }

    /**
     * Persist a failed AI usage record independently of the
     * transaction that was generating the summary.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(UUID restaurantId) {
        AiUsageLog usageLog = new AiUsageLog();
        usageLog.setRestaurantId(restaurantId);
        usageLog.setStatus("FAILED");

        usageLogs.save(usageLog);
    }
}
