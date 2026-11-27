
package com.spiceroute.feedback.feedback.analytics;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class AnalyticsRepository {

    private record Built(String where, MapSqlParameterSource params) {}

    private final NamedParameterJdbcTemplate jdbc;

    public AnalyticsRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public SummaryResponse summary(AnalyticsFilter f) {
        Built w = conditions(f, true, true);

        String sql = """
                SELECT COUNT(*) AS total,
                       AVG(f.rating)::float8 AS avg_rating,
                       COUNT(*) FILTER (WHERE f.comment IS NOT NULL AND btrim(f.comment) <> '') AS with_comments,
                       COUNT(*) FILTER (WHERE f.rating = 1) AS r1,
                       COUNT(*) FILTER (WHERE f.rating = 2) AS r2,
                       COUNT(*) FILTER (WHERE f.rating = 3) AS r3,
                       COUNT(*) FILTER (WHERE f.rating = 4) AS r4,
                       COUNT(*) FILTER (WHERE f.rating = 5) AS r5
                FROM feedbacks f
                WHERE %s
                """.formatted(w.where());

        return jdbc.queryForObject(sql, w.params(), (rs, i) -> new SummaryResponse(
                rs.getLong("total"),
                round(rs.getObject("avg_rating", Double.class)),
                rs.getLong("with_comments"),
                counts(rs)));
    }

    /** Unit must be one of day, week, month (validated by the service). */
    public List<TrendPoint> trend(AnalyticsFilter f, String unit, String timezone) {
        Built w = conditions(f, true, true);
        w.params().addValue("tz", timezone);

        String sql = "SELECT date_trunc('" + unit + "', f.created_at AT TIME ZONE :tz) AS bucket, "
                + "COUNT(*) AS cnt, AVG(f.rating)::float8 AS avg_rating "
                + "FROM feedbacks f WHERE " + w.where() + " GROUP BY 1 ORDER BY 1";

        return jdbc.query(sql, w.params(), (rs, i) -> new TrendPoint(
                rs.getTimestamp("bucket").toLocalDateTime().toLocalDate(),
                rs.getLong("cnt"),
                round(rs.getObject("avg_rating", Double.class))));
    }

    /** Every branch in scope, including branches with no feedback in the period. */
    public List<BranchStat> branches(AnalyticsFilter f) {
        Built feedbackFilters = conditions(f, false, false);
        MapSqlParameterSource p = feedbackFilters.params();

        String restaurantCondition = "";
        if (!f.platformWide()) {
            restaurantCondition = " WHERE b.restaurant_id = :rid";
            p.addValue("rid", f.restaurantId());
        }

        String scopeSql = "";
        if (f.scope() != null) {
            if (f.scope().isEmpty()) {
                scopeSql = " AND false";
            } else {
                scopeSql = " AND b.id IN (:scope)";
                p.addValue("scope", f.scope());
            }
        }

        String on = feedbackFilters.where().isEmpty()
                ? ""
                : " AND " + feedbackFilters.where();

        String sql = "SELECT b.id, b.name, COUNT(f.id) AS cnt, AVG(f.rating)::float8 AS avg_rating, "
                + "COUNT(f.id) FILTER (WHERE f.rating = 1) AS r1, "
                + "COUNT(f.id) FILTER (WHERE f.rating = 2) AS r2, "
                + "COUNT(f.id) FILTER (WHERE f.rating = 3) AS r3, "
                + "COUNT(f.id) FILTER (WHERE f.rating = 4) AS r4, "
                + "COUNT(f.id) FILTER (WHERE f.rating = 5) AS r5 "
                + "FROM branches b LEFT JOIN feedbacks f ON f.branch_id = b.id" + on
                + restaurantCondition + scopeSql
                + " GROUP BY b.id, b.name ORDER BY b.name";

        return jdbc.query(sql, p, (rs, i) -> new BranchStat(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getLong("cnt"),
                round(rs.getObject("avg_rating", Double.class)),
                counts(rs)));
    }

    private static Built conditions(
            AnalyticsFilter f,
            boolean tenantAndScope,
            boolean branchAndTable
    ) {
        List<String> parts = new ArrayList<>();
        MapSqlParameterSource p = new MapSqlParameterSource();

        if (tenantAndScope) {
            if (!f.platformWide()) {
                parts.add("f.restaurant_id = :rid");
                p.addValue("rid", f.restaurantId());
            }

            if (f.scope() != null) {
                if (f.scope().isEmpty()) {
                    parts.add("false");
                } else {
                    parts.add("f.branch_id IN (:scope)");
                    p.addValue("scope", f.scope());
                }
            }
        }

        if (branchAndTable) {
            if (f.branchId() != null) {
                parts.add("f.branch_id = :bid");
                p.addValue("bid", f.branchId());
            }

            if (f.tableId() != null) {
                parts.add("f.table_id = :tid");
                p.addValue("tid", f.tableId());
            }
        }

        if (f.minRating() != null) {
            parts.add("f.rating >= :minr");
            p.addValue("minr", f.minRating());
        }

        if (f.maxRating() != null) {
            parts.add("f.rating <= :maxr");
            p.addValue("maxr", f.maxRating());
        }

        if (f.from() != null) {
            parts.add("f.created_at >= :from");
            p.addValue("from", OffsetDateTime.ofInstant(f.from(), ZoneOffset.UTC));
        }

        if (f.toExclusive() != null) {
            parts.add("f.created_at < :to");
            p.addValue("to", OffsetDateTime.ofInstant(f.toExclusive(), ZoneOffset.UTC));
        }

        return new Built(String.join(" AND ", parts), p);
    }

    private static Map<Integer, Long> counts(ResultSet rs) throws SQLException {
        Map<Integer, Long> counts = new LinkedHashMap<>();
        for (int star = 1; star <= 5; star++) {
            counts.put(star, rs.getLong("r" + star));
        }
        return counts;
    }

    private static Double round(Double value) {
        return value == null ? null : Math.round(value * 100.0) / 100.0;
    }
}
