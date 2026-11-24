package com.spiceroute.feedback.admin;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Raw SQL aggregations for the super-admin dashboard.
 * Uses NamedParameterJdbcTemplate so queries stay readable and all values are bound.
 */
@Repository
public class AdminRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AdminRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** One row per restaurant: name, slug, status, owner email, branch count, feedback count, createdAt. */
    public List<AdminRestaurantRow> listRestaurants() {
        String sql = """
                SELECT r.id,
                       r.name,
                       r.slug,
                       r.status,
                       r.created_at,
                       u.name        AS owner_name,
                       u.email       AS owner_email,
                       COUNT(DISTINCT b.id)  AS branch_count,
                       COUNT(DISTINCT f.id)  AS feedback_count
                FROM restaurants r
                LEFT JOIN users    u ON u.id = r.owner_id
                LEFT JOIN branches b ON b.restaurant_id = r.id
                LEFT JOIN feedbacks f ON f.restaurant_id = r.id
                GROUP BY r.id, r.name, r.slug, r.status, r.created_at, u.name, u.email
                ORDER BY r.created_at DESC
                """;
        return jdbc.query(sql, new MapSqlParameterSource(), (rs, i) ->
                new AdminRestaurantRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getString("slug"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getString("owner_name"),
                        rs.getString("owner_email"),
                        rs.getLong("branch_count"),
                        rs.getLong("feedback_count")));
    }

    /** Platform-wide totals for the stats card row. */
    public AdminStatsRow platformStats() {
        java.time.OffsetDateTime startOfToday = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.DAYS);
        String sql = """
                SELECT
                  (SELECT COUNT(*) FROM restaurants)   AS restaurant_count,
                  (SELECT COUNT(*) FROM users)         AS user_count,
                  (SELECT COUNT(*) FROM feedbacks)     AS feedback_total,
                  (SELECT COUNT(*) FROM feedbacks
                    WHERE created_at >= :today)        AS feedback_today
                """;
        return jdbc.queryForObject(sql,
                new MapSqlParameterSource("today", startOfToday),
                (rs, i) -> new AdminStatsRow(
                        rs.getLong("restaurant_count"),
                        rs.getLong("user_count"),
                        rs.getLong("feedback_total"),
                        rs.getLong("feedback_today")));
    }

    /** All users across the platform with their restaurant name. */
    public List<AdminUserRow> listUsers() {
        String sql = """
                SELECT u.id,
                       u.name,
                       u.email,
                       u.role,
                       u.is_active,
                       u.created_at,
                       r.name AS restaurant_name
                FROM users u
                LEFT JOIN restaurants r ON r.id = u.restaurant_id
                ORDER BY u.created_at DESC
                """;
        return jdbc.query(sql, new MapSqlParameterSource(), (rs, i) ->
                new AdminUserRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getBoolean("is_active"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getString("restaurant_name")));
    }

    /* ── Internal row types (package-private — only used by AdminService) ── */

    record AdminRestaurantRow(UUID id, String name, String slug, String status, Instant createdAt,
                              String ownerName, String ownerEmail,
                              long branchCount, long feedbackCount) {}

    record AdminStatsRow(long restaurantCount, long userCount,
                         long feedbackTotal, long feedbackToday) {}

    record AdminUserRow(UUID id, String name, String email, String role,
                        boolean active, Instant createdAt, String restaurantName) {}
}
