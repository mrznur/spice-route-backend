package com.spiceroute.feedback.feedback;

import com.spiceroute.feedback.common.ApiException;
import com.spiceroute.feedback.common.SlidingWindowRateLimiter;
import com.spiceroute.feedback.domain.Branch;
import com.spiceroute.feedback.domain.DiningTable;
import com.spiceroute.feedback.domain.Feedback;
import com.spiceroute.feedback.domain.RestaurantStatus;
import com.spiceroute.feedback.repository.BranchRepository;
import com.spiceroute.feedback.repository.DiningTableRepository;
import com.spiceroute.feedback.repository.FeedbackRepository;
import com.spiceroute.feedback.repository.FeedbackSpecs;
import com.spiceroute.feedback.repository.RestaurantRepository;
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

@Service
public class FeedbackService {

    private static final Set<String> ALLOWED_LANGUAGES = Set.of("en", "bn", "banglish");
    private static final Duration TABLE_WINDOW = Duration.ofMinutes(10);
    private static final Duration IP_WINDOW = Duration.ofHours(1);

    private final FeedbackRepository feedbacks;
    private final DiningTableRepository tables;
    private final BranchRepository branches;
    private final RestaurantRepository restaurants;
    private final SlidingWindowRateLimiter limiter;
    private final int perTableLimit;
    private final int perIpLimit;

    public FeedbackService(FeedbackRepository feedbacks, DiningTableRepository tables,
                           BranchRepository branches, RestaurantRepository restaurants,
                           SlidingWindowRateLimiter limiter,
                           @Value("${app.rate-limit.per-table:5}") int perTableLimit,
                           @Value("${app.rate-limit.per-ip:60}") int perIpLimit) {
        this.feedbacks = feedbacks;
        this.tables = tables;
        this.branches = branches;
        this.restaurants = restaurants;
        this.limiter = limiter;
        this.perTableLimit = perTableLimit;
        this.perIpLimit = perIpLimit;
    }

    /** Public endpoint — no auth required. QR token identifies the table. */
    @Transactional
    public FeedbackResponse submit(SubmitFeedbackRequest req) {
        String token = req.qrToken().trim();

        // limits run before any database lookup so abusive clients are cheap to reject
        String ip = clientIp();
        if (!limiter.tryAcquire("ip:" + ip, perIpLimit, IP_WINDOW)
                || !limiter.tryAcquire("table:" + ip + ":" + token, perTableLimit, TABLE_WINDOW)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many submissions. Please try again in a few minutes.");
        }

        DiningTable table = tables.findByQrToken(token)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "QR code not found"));
        if (!table.isActive()) {
            throw new ApiException(HttpStatus.GONE, "This table is no longer active");
        }
        // an inactive branch or a suspended restaurant must not keep collecting feedback
        Branch branch = branches.findById(table.getBranchId()).filter(Branch::isActive)
                .orElseThrow(() -> new ApiException(HttpStatus.GONE, "This QR code is no longer active"));
        restaurants.findById(branch.getRestaurantId())
                .filter(r -> r.getStatus() == RestaurantStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.GONE, "This QR code is no longer active"));

        String comment = blankToNull(req.comment());
        String name = blankToNull(req.customerName());
        String phone = blankToNull(req.customerPhone());

        // anonymous means anonymous: name and phone are never stored
        boolean anonymous = Boolean.TRUE.equals(req.anonymous()) || (name == null && phone == null);
        if (anonymous) {
            name = null;
            phone = null;
        }

        // the comment text decides the tag; the client's hint is only used when there is no comment
        String language;
        if (comment != null) {
            language = LanguageDetector.detect(comment);
        } else {
            language = (req.language() != null && ALLOWED_LANGUAGES.contains(req.language()))
                    ? req.language() : "en";
        }

        Feedback f = new Feedback();
        f.setRestaurantId(table.getRestaurantId());
        f.setBranchId(table.getBranchId());
        f.setTableId(table.getId());
        f.setRating(req.rating().shortValue());
        f.setComment(comment);
        f.setCustomerName(name);
        f.setCustomerPhone(phone);
        f.setAnonymous(anonymous);
        f.setLanguage(language);

        return FeedbackResponse.from(feedbacks.save(f));
    }

    /** Paginated list for the owner/manager dashboard. */
    @Transactional(readOnly = true)
    public FeedbackPageResponse list(UUID branchId, Short minRating, Short maxRating,
                                     Instant from, Instant to, int page, int size) {
        UUID restaurantId = CurrentUser.restaurantId();
        PageRequest pr = PageRequest.of(page, Math.min(size, 100));
        var spec = FeedbackSpecs.search(restaurantId, branchId, minRating, maxRating, from, to);
        Page<Feedback> result = feedbacks.findAll(spec, pr);
        List<FeedbackResponse> content = result.getContent().stream().map(FeedbackResponse::from).toList();
        return new FeedbackPageResponse(content, result.getTotalElements(), result.getTotalPages(), page, size);
    }

    /** Summary stats for dashboard/performance. */
    @Transactional(readOnly = true)
    public FeedbackStatsResponse stats(UUID branchId) {
        UUID restaurantId = CurrentUser.restaurantId();

        // Count per rating using the paged query filtered per star
        long five  = countByRating(restaurantId, branchId, (short) 5);
        long four  = countByRating(restaurantId, branchId, (short) 4);
        long three = countByRating(restaurantId, branchId, (short) 3);
        long two   = countByRating(restaurantId, branchId, (short) 2);
        long one   = countByRating(restaurantId, branchId, (short) 1);
        long total = five + four + three + two + one;
        Double avg = feedbacks.avgRating(restaurantId, branchId);

        return new FeedbackStatsResponse(total, avg, five, four, three, two, one);
    }

    private long countByRating(UUID restaurantId, UUID branchId, short rating) {
        var spec = FeedbackSpecs.search(restaurantId, branchId, rating, rating, null, null);
        return feedbacks.count(spec);
    }

    /** Behind a reverse proxy, set server.forward-headers-strategy=framework so this is the real client IP. */
    private static String clientIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest().getRemoteAddr();
        }
        return "unknown";
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
