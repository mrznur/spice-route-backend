package com.spiceroute.feedback.publicapi;

import com.spiceroute.feedback.common.ApiException;
import com.spiceroute.feedback.domain.Branch;
import com.spiceroute.feedback.domain.DiningTable;
import com.spiceroute.feedback.domain.Restaurant;
import com.spiceroute.feedback.domain.RestaurantStatus;
import com.spiceroute.feedback.repository.BranchRepository;
import com.spiceroute.feedback.repository.DiningTableRepository;
import com.spiceroute.feedback.repository.RestaurantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicTableService {

    private final DiningTableRepository tables;
    private final BranchRepository branches;
    private final RestaurantRepository restaurants;

    public PublicTableService(DiningTableRepository tables, BranchRepository branches,
                              RestaurantRepository restaurants) {
        this.tables = tables;
        this.branches = branches;
        this.restaurants = restaurants;
    }

    /** Unknown, inactive, or suspended all look the same to the outside: 404. */
    @Transactional(readOnly = true)
    public PublicTableInfo resolve(String qrToken) {
        DiningTable table = tables.findByQrToken(qrToken).filter(DiningTable::isActive)
                .orElseThrow(PublicTableService::notFound);
        Branch branch = branches.findById(table.getBranchId()).filter(Branch::isActive)
                .orElseThrow(PublicTableService::notFound);
        Restaurant restaurant = restaurants.findById(branch.getRestaurantId())
                .filter(r -> r.getStatus() == RestaurantStatus.ACTIVE)
                .orElseThrow(PublicTableService::notFound);
        return new PublicTableInfo(restaurant.getName(), branch.getName(), table.getLabel());
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "QR code not found or inactive");
    }
}
