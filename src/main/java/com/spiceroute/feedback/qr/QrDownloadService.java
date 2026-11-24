package com.spiceroute.feedback.qr;

import com.spiceroute.feedback.branch.BranchService;
import com.spiceroute.feedback.common.ApiException;
import com.spiceroute.feedback.domain.Branch;
import com.spiceroute.feedback.domain.DiningTable;
import com.spiceroute.feedback.domain.Restaurant;
import com.spiceroute.feedback.repository.DiningTableRepository;
import com.spiceroute.feedback.repository.RestaurantRepository;
import com.spiceroute.feedback.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class QrDownloadService {

    public record Download(String filename, byte[] bytes) {}

    private static final Comparator<DiningTable> ORDER =
            Comparator.comparingInt((DiningTable t) -> t.getLabel().length())
                    .thenComparing(DiningTable::getLabel, String.CASE_INSENSITIVE_ORDER);

    private final DiningTableRepository tables;
    private final RestaurantRepository restaurants;
    private final BranchService branchService;
    private final QrService qr;

    public QrDownloadService(DiningTableRepository tables, RestaurantRepository restaurants,
                             BranchService branchService, QrService qr) {
        this.tables = tables;
        this.restaurants = restaurants;
        this.branchService = branchService;
        this.qr = qr;
    }

    @Transactional(readOnly = true)
    public Download tablePng(UUID tableId) {
        DiningTable table = tables.findByIdAndRestaurantId(tableId, CurrentUser.restaurantId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Table not found"));
        Branch branch = branchService.requireAccessible(table.getBranchId());
        String filename = "qr-" + slug(branch.getName()) + "-" + slug(table.getLabel()) + ".png";
        return new Download(filename, qr.png(table.getQrToken(), 800));
    }

    @Transactional(readOnly = true)
    public Download branchPdf(UUID branchId) {
        Branch branch = branchService.requireAccessible(branchId);
        Restaurant restaurant = restaurants.findById(branch.getRestaurantId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Restaurant not found"));
        List<DiningTable> active = tables.findByBranchIdAndRestaurantId(branch.getId(), branch.getRestaurantId())
                .stream().filter(DiningTable::isActive).sorted(ORDER).toList();
        if (active.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "No active tables in this branch");
        }
        return new Download("qr-" + slug(branch.getName()) + ".pdf",
                qr.branchPdf(restaurant.getName(), branch.getName(), active));
    }

    private static String slug(String value) {
        String s = value.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return s.isEmpty() ? "x" : s;
    }
}
