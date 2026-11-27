
package com.spiceroute.feedback.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class AiUsageLogTransactionTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AiUsageLogService usageLogService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void failureLogSurvivesOuterTransactionRollback() {
        UUID restaurantId = UUID.randomUUID();
        String slug = "transaction-test-" + restaurantId;

        jdbc.update(
            """
            INSERT INTO restaurants (id, name, slug)
            VALUES (?, ?, ?)
            """,
            restaurantId,
            "Transaction Test Restaurant",
            slug
        );

        try {
            TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

            transaction.executeWithoutResult(status -> {
                usageLogService.recordFailure(restaurantId);

                // Roll back the outer transaction deliberately.
                status.setRollbackOnly();
            });

            Integer count = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM ai_usage_logs
                WHERE restaurant_id = ?
                  AND status = 'FAILED'
                """,
                Integer.class,
                restaurantId
            );

            assertEquals(1, count,
                "The FAILED usage log should survive the outer rollback.");

        } finally {
            jdbc.update(
                "DELETE FROM restaurants WHERE id = ?",
                restaurantId
            );
        }
    }
}
