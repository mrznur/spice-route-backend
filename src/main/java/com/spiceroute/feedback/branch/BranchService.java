package com.spiceroute.feedback.branch;

import com.spiceroute.feedback.common.ApiException;
import com.spiceroute.feedback.domain.Branch;
import com.spiceroute.feedback.domain.ManagerBranch;
import com.spiceroute.feedback.domain.Role;
import com.spiceroute.feedback.repository.BranchRepository;
import com.spiceroute.feedback.repository.ManagerBranchRepository;
import com.spiceroute.feedback.security.AuthPrincipal;
import com.spiceroute.feedback.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BranchService {

    private final BranchRepository branches;
    private final ManagerBranchRepository managerBranches;

    public BranchService(BranchRepository branches, ManagerBranchRepository managerBranches) {
        this.branches = branches;
        this.managerBranches = managerBranches;
    }

    /** Owners see every branch of their restaurant; managers only their assigned branches. */
    @Transactional(readOnly = true)
    public List<BranchResponse> list() {
        AuthPrincipal me = CurrentUser.get();
        UUID restaurantId = CurrentUser.restaurantId();
        List<Branch> result;
        if (me.role() == Role.MANAGER) {
            List<UUID> ids = managerBranches.findByUserId(me.userId()).stream()
                    .map(ManagerBranch::getBranchId).toList();
            result = ids.isEmpty() ? List.of()
                    : branches.findByRestaurantIdAndIdInOrderByNameAsc(restaurantId, ids);
        } else {
            result = branches.findByRestaurantIdOrderByNameAsc(restaurantId);
        }
        return result.stream().map(BranchResponse::from).toList();
    }

    @Transactional
    public BranchResponse create(BranchRequest req) {
        Branch branch = new Branch();
        branch.setRestaurantId(CurrentUser.restaurantId());
        branch.setName(req.name().trim());
        branch.setAddress(req.address() == null ? null : req.address().trim());
        return BranchResponse.from(branches.save(branch));
    }

    @Transactional
    public BranchResponse update(UUID id, BranchRequest req) {
        Branch branch = branches.findByIdAndRestaurantId(id, CurrentUser.restaurantId())
                .orElseThrow(BranchService::notFound);
        branch.setName(req.name().trim());
        branch.setAddress(req.address() == null ? null : req.address().trim());
        if (req.active() != null) branch.setActive(req.active());
        return BranchResponse.from(branch);
    }

    @Transactional
    public void delete(UUID id) {
        Branch branch = branches.findByIdAndRestaurantId(id, CurrentUser.restaurantId())
                .orElseThrow(BranchService::notFound);
        // Soft-delete: deactivate instead of hard-delete so that all historical
        // feedback rows (which CASCADE-DELETE on hard delete) are preserved.
        branch.setActive(false);
    }

    /**
     * The single gate for branch-scoped data: the branch must belong to the caller's restaurant,
     * and a manager must also be assigned to it. Anything else is reported as 404 so other
     * restaurants' ids are never confirmed to exist. Reuse this in every branch-scoped feature.
     */
    @Transactional(readOnly = true)
    public Branch requireAccessible(UUID branchId) {
        AuthPrincipal me = CurrentUser.get();
        Branch branch = branches.findByIdAndRestaurantId(branchId, CurrentUser.restaurantId())
                .orElseThrow(BranchService::notFound);
        if (me.role() == Role.MANAGER && !managerBranches.existsByUserIdAndBranchId(me.userId(), branchId)) {
            throw notFound();
        }
        return branch;
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "Branch not found");
    }
}
