
package com.spiceroute.feedback.feedback;

import com.spiceroute.feedback.common.ApiException;
import com.spiceroute.feedback.common.SlidingWindowRateLimiter;
import com.spiceroute.feedback.domain.Branch;
import com.spiceroute.feedback.domain.DiningTable;
import com.spiceroute.feedback.domain.Feedback;
import com.spiceroute.feedback.domain.RestaurantStatus;
import com.spiceroute.feedback.domain.Role;
import com.spiceroute.feedback.repository.BranchRepository;
import com.spiceroute.feedback.repository.DiningTableRepository;
import com.spiceroute.feedback.repository.FeedbackRepository;
import com.spiceroute.feedback.repository.FeedbackSpecs;
import com.spiceroute.feedback.repository.ManagerBranchRepository;
import com.spiceroute.feedback.repository.RestaurantRepository;
import com.spiceroute.feedback.security.AuthPrincipal;
import com.spiceroute.feedback.security.CurrentUser;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FeedbackService {

    private static final Set<String> ALLOWED_LANGUAGES =
            Set.of("en", "bn", "banglish");

    private static final Duration TABLE_WINDOW = Duration.ofMinutes(10);
    private static final Duration IP_WINDOW = Duration.ofHours(1);

    private final FeedbackRepository feedbacks;
    private final DiningTableRepository tables;
    private final BranchRepository branches;
    private final RestaurantRepository restaurants;
    private final ManagerBranchRepository managerBranches;
    private final SlidingWindowRateLimiter limiter;
    private final int perTableLimit;
    private final int perIpLimit;

    public FeedbackService(
            FeedbackRepository feedbacks,
            DiningTableRepository tables,
            BranchRepository branches,
            RestaurantRepository restaurants,
            ManagerBranchRepository managerBranches,
            SlidingWindowRateLimiter limiter,
            @Value("${app.rate-limit.per-table:5}") int perTableLimit,
            @Value("${app.rate-limit.per-ip:60}") int perIpLimit
    ) {
        this.feedbacks = feedbacks;
        this.tables = tables;
        this.branches = branches;
        this.restaurants = restaurants;
        this.managerBranches = managerBranches;
        this.limiter = limiter;
        this.perTableLimit = perTableLimit;
        this.perIpLimit = perIpLimit;
    }

    /**
     * Public endpoint — no authentication required.
     * QR token identifies the table.
     */
    @Transactional
    public FeedbackResponse submit(SubmitFeedbackRequest req) {
        String token = req.qrToken().trim();

        String ip = clientIp();

        if (!limiter.tryAcquire("ip:" + ip, perIpLimit, IP_WINDOW)
                || !limiter.tryAcquire(
                        "table:" + ip + ":" + token,
                        perTableLimit,
                        TABLE_WINDOW
                )) {
            throw new ApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many submissions. Please try again in a few minutes."
            );
        }

        DiningTable table = tables.findByQrToken(token)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "QR code not found"
                ));

        if (!table.isActive()) {
            throw new ApiException(
                    HttpStatus.GONE,
                    "This table is no longer active"
            );
        }

        Branch branch = branches.findById(table.getBranchId())
                .filter(Branch::isActive)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.GONE,
                        "This QR code is no longer active"
                ));

        restaurants.findById(branch.getRestaurantId())
                .filter(r -> r.getStatus() == RestaurantStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.GONE,
                        "This QR code is no longer active"
                ));

        String comment = blankToNull(req.comment());
        String name = blankToNull(req.customerName());
        String phone = blankToNull(req.customerPhone());

        boolean anonymous =
                Boolean.TRUE.equals(req.anonymous())
                        || (name == null && phone == null);

        if (anonymous) {
            name = null;
            phone = null;
        }

        String language;

        if (comment != null) {
            language = LanguageDetector.detect(comment);
        } else {
            language = req.language() != null
                    && ALLOWED_LANGUAGES.contains(req.language())
                    ? req.language()
                    : "en";
        }

        Feedback feedback = new Feedback();
        feedback.setRestaurantId(table.getRestaurantId());
        feedback.setBranchId(table.getBranchId());
        feedback.setTableId(table.getId());
        feedback.setRating(req.rating().shortValue());
        feedback.setComment(comment);
        feedback.setCustomerName(name);
        feedback.setCustomerPhone(phone);
        feedback.setAnonymous(anonymous);
        feedback.setLanguage(language);

        return FeedbackResponse.from(feedbacks.save(feedback));
    }

    /**
     * Paginated feedback list.
     *
     * SUPER_ADMIN sees all restaurants.
     * OWNER sees all branches in their restaurant.
     * MANAGER sees only assigned branches.
     */
    @Transactional(readOnly = true)
    public FeedbackPageResponse list(
            UUID branchId,
            Short minRating,
            Short maxRating,
            Instant from,
            Instant to,
            int page,
            int size
    ) {
        AuthPrincipal principal = CurrentUser.get();
        Role role = principal.role();

        UUID restaurantId = role == Role.SUPER_ADMIN
                ? null
                : CurrentUser.restaurantId();

        Set<UUID> allowedBranchIds = getAllowedBranchIds(principal);

        validateBranchFilter(branchId, allowedBranchIds, role, restaurantId);

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));

        PageRequest pageRequest = PageRequest.of(safePage, safeSize);

        var spec = role == Role.SUPER_ADMIN
                ? FeedbackSpecs.searchPlatformWide(
                        branchId, minRating, maxRating, from, to
                )
                : FeedbackSpecs.search(
                        restaurantId,
                        allowedBranchIds,
                        branchId,
                        minRating,
                        maxRating,
                        from,
                        to
                );

        Page<Feedback> result = feedbacks.findAll(spec, pageRequest);

        List<FeedbackResponse> content = result.getContent()
                .stream()
                .map(FeedbackResponse::from)
                .toList();

        return new FeedbackPageResponse(
                content,
                result.getTotalElements(),
                result.getTotalPages(),
                safePage,
                safeSize
        );
    }

    /**
     * Dashboard feedback statistics.
     * Uses the same authorization scope as list().
     */
    @Transactional(readOnly = true)
    public FeedbackStatsResponse stats(UUID branchId) {
        AuthPrincipal principal = CurrentUser.get();
        Role role = principal.role();

        UUID restaurantId = role == Role.SUPER_ADMIN
                ? null
                : CurrentUser.restaurantId();

        Set<UUID> allowedBranchIds = getAllowedBranchIds(principal);

        validateBranchFilter(branchId, allowedBranchIds, role, restaurantId);

        long five = countByRating(
                restaurantId, allowedBranchIds, branchId, role, (short) 5
        );
        long four = countByRating(
                restaurantId, allowedBranchIds, branchId, role, (short) 4
        );
        long three = countByRating(
                restaurantId, allowedBranchIds, branchId, role, (short) 3
        );
        long two = countByRating(
                restaurantId, allowedBranchIds, branchId, role, (short) 2
        );
        long one = countByRating(
                restaurantId, allowedBranchIds, branchId, role, (short) 1
        );

        long total = five + four + three + two + one;

        Double avg = total == 0
                ? null
                : (5.0 * five
                    + 4.0 * four
                    + 3.0 * three
                    + 2.0 * two
                    + one) / total;

        return new FeedbackStatsResponse(
                total, avg, five, four, three, two, one
        );
    }

    private long countByRating(
            UUID restaurantId,
            Set<UUID> allowedBranchIds,
            UUID branchId,
            Role role,
            short rating
    ) {
        var spec = role == Role.SUPER_ADMIN
                ? FeedbackSpecs.searchPlatformWide(
                        branchId, rating, rating, null, null
                )
                : FeedbackSpecs.search(
                        restaurantId,
                        allowedBranchIds,
                        branchId,
                        rating,
                        rating,
                        null,
                        null
                );

        return feedbacks.count(spec);
    }

    /**
     * OWNER: null means all branches in their restaurant.
     * MANAGER: assigned branch IDs only.
     * SUPER_ADMIN: null means platform-wide; handled separately.
     */
    private Set<UUID> getAllowedBranchIds(AuthPrincipal principal) {
        if (principal.role() == Role.SUPER_ADMIN
                || principal.role() == Role.OWNER) {
            return null;
        }

        if (principal.role() == Role.MANAGER) {
            return managerBranches.findByUserId(principal.userId())
                    .stream()
                    .map(managerBranch -> managerBranch.getBranchId())
                    .collect(Collectors.toSet());
        }

        throw new ApiException(
                HttpStatus.FORBIDDEN,
                "This account cannot access feedback"
        );
    }

    private void validateBranchFilter(
            UUID branchId,
            Set<UUID> allowedBranchIds,
            Role role,
            UUID restaurantId
    ) {
        if (branchId == null) {
            return;
        }

        boolean branchAccessible = branches.findById(branchId)
                .map(branch -> {
                    if (role != Role.SUPER_ADMIN
                            && !branch.getRestaurantId().equals(restaurantId)) {
                        return false;
                    }

                    return allowedBranchIds == null
                            || allowedBranchIds.contains(branchId);
                })
                .orElse(false);

        if (!branchAccessible) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "Branch not found"
            );
        }
    }

    /**
     * Behind a reverse proxy, configure
     * server.forward-headers-strategy=framework.
     */
    private static String clientIp() {
        if (RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest().getRemoteAddr();
        }

        return "unknown";
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
