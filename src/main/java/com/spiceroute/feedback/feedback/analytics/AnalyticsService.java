
package com.spiceroute.feedback.feedback.analytics;

import com.spiceroute.feedback.branch.BranchService;
import com.spiceroute.feedback.common.ApiException;
import com.spiceroute.feedback.domain.DiningTable;
import com.spiceroute.feedback.domain.ManagerBranch;
import com.spiceroute.feedback.domain.Role;
import com.spiceroute.feedback.repository.DiningTableRepository;
import com.spiceroute.feedback.repository.ManagerBranchRepository;
import com.spiceroute.feedback.security.AuthPrincipal;
import com.spiceroute.feedback.security.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private static final Set<String> INTERVALS = Set.of("day", "week", "month");
    private static final long MAX_RANGE_DAYS = 731;
    private static final int DEFAULT_TREND_DAYS = 30;

    private final AnalyticsRepository repository;
    private final BranchService branchService;
    private final DiningTableRepository tables;
    private final ManagerBranchRepository managerBranches;
    private final ZoneId zone;

    public AnalyticsService(AnalyticsRepository repository, BranchService branchService,
                            DiningTableRepository tables, ManagerBranchRepository managerBranches,
                            @Value("${app.timezone:Asia/Dhaka}") String timezone) {
        this.repository = repository;
        this.branchService = branchService;
        this.tables = tables;
        this.managerBranches = managerBranches;
        this.zone = ZoneId.of(timezone);
    }

    public SummaryResponse summary(UUID branchId, UUID tableId, Integer minRating, Integer maxRating,
                                   LocalDate from, LocalDate to) {
        return repository.summary(filter(branchId, tableId, minRating, maxRating, from, to));
    }

    /** Defaults to the last 30 days. Day buckets are zero-filled so charts have no gaps. */
    public List<TrendPoint> trend(UUID branchId, UUID tableId, Integer minRating, Integer maxRating,
                                  LocalDate from, LocalDate to, String interval) {
        String unit = interval == null ? "day" : interval.trim().toLowerCase();
        if (!INTERVALS.contains(unit)) {
            throw bad("interval must be day, week or month");
        }
        LocalDate effectiveTo = to != null ? to : LocalDate.now(zone);
        LocalDate effectiveFrom = from != null ? from : effectiveTo.minusDays(DEFAULT_TREND_DAYS - 1L);

        AnalyticsFilter f = filter(branchId, tableId, minRating, maxRating, effectiveFrom, effectiveTo);
        List<TrendPoint> points = repository.trend(f, unit, zone.getId());
        if (!unit.equals("day")) return points;

        Map<LocalDate, TrendPoint> byDay = new HashMap<>();
        points.forEach(p -> byDay.put(p.period(), p));
        List<TrendPoint> filled = new ArrayList<>();
        for (LocalDate d = effectiveFrom; !d.isAfter(effectiveTo); d = d.plusDays(1)) {
            filled.add(byDay.getOrDefault(d, new TrendPoint(d, 0, null)));
        }
        return filled;
    }

    /** Branch comparison for the dates and ratings given; branch and table filters do not apply here. */
    public List<BranchStat> branches(Integer minRating, Integer maxRating, LocalDate from, LocalDate to) {
        return repository.branches(filter(null, null, minRating, maxRating, from, to));
    }

    private AnalyticsFilter filter(UUID branchId, UUID tableId, Integer minRating, Integer maxRating,
                                   LocalDate from, LocalDate to) {
        AuthPrincipal me = CurrentUser.get();
        boolean platformWide = me.role() == Role.SUPER_ADMIN;
        UUID restaurantId = platformWide ? null : CurrentUser.restaurantId();

        if (minRating != null && (minRating < 1 || minRating > 5)) throw bad("minRating must be between 1 and 5");
        if (maxRating != null && (maxRating < 1 || maxRating > 5)) throw bad("maxRating must be between 1 and 5");
        if (minRating != null && maxRating != null && minRating > maxRating) {
            throw bad("minRating must not be greater than maxRating");
        }
        if (from != null && to != null) {
            if (from.isAfter(to)) throw bad("from must not be after to");
            if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) throw bad("Date range is too large (max 2 years)");
        }

        // Managers are limited to their assigned branches, enforced in every query.
        Set<UUID> scope = null;
        if (me.role() == Role.MANAGER) {
            scope = managerBranches.findByUserId(me.userId()).stream()
                    .map(ManagerBranch::getBranchId).collect(Collectors.toSet());
        }

        // An unknown, foreign or unassigned branch/table is a 404, never an empty 200.
        if (branchId != null) {
            branchService.requireAccessible(branchId);
        }

        if (tableId != null) {
            DiningTable table = (platformWide
                    ? tables.findById(tableId)
                    : tables.findByIdAndRestaurantId(tableId, restaurantId))
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Table not found"));

            branchService.requireAccessible(table.getBranchId());
        }

        Instant fromInstant = from == null ? null : from.atStartOfDay(zone).toInstant();
        Instant toExclusive = to == null ? null : to.plusDays(1).atStartOfDay(zone).toInstant();

        return new AnalyticsFilter(restaurantId, platformWide, scope, branchId, tableId,
                minRating, maxRating, fromInstant, toExclusive);
    }

    private static ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}
