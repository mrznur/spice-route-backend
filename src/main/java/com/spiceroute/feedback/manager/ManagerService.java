package com.spiceroute.feedback.manager;

import com.spiceroute.feedback.common.ApiException;
import com.spiceroute.feedback.domain.Branch;
import com.spiceroute.feedback.domain.ManagerBranch;
import com.spiceroute.feedback.domain.Role;
import com.spiceroute.feedback.domain.User;
import com.spiceroute.feedback.repository.BranchRepository;
import com.spiceroute.feedback.repository.ManagerBranchRepository;
import com.spiceroute.feedback.repository.RefreshTokenRepository;
import com.spiceroute.feedback.repository.UserRepository;
import com.spiceroute.feedback.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ManagerService {

    private final UserRepository users;
    private final BranchRepository branches;
    private final ManagerBranchRepository managerBranches;
    private final RefreshTokenRepository refreshTokenRepo;
    private final PasswordEncoder encoder;

    public ManagerService(UserRepository users, BranchRepository branches,
                          ManagerBranchRepository managerBranches,
                          RefreshTokenRepository refreshTokenRepo,
                          PasswordEncoder encoder) {
        this.users = users;
        this.branches = branches;
        this.managerBranches = managerBranches;
        this.refreshTokenRepo = refreshTokenRepo;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<ManagerResponse> list() {
        UUID restaurantId = CurrentUser.restaurantId();
        List<User> managers = users.findByRestaurantIdAndRoleOrderByNameAsc(restaurantId, Role.MANAGER);
        if (managers.isEmpty()) return List.of();

        // All branch-id assignments for these managers in one query
        Map<UUID, List<UUID>> assignmentsByUser = managerBranches
                .findByUserIdIn(managers.stream().map(User::getId).toList()).stream()
                .collect(Collectors.groupingBy(ManagerBranch::getUserId,
                        Collectors.mapping(ManagerBranch::getBranchId, Collectors.toList())));

        // Collect every unique branch id across all managers, then fetch names in one query
        Set<UUID> allBranchIds = assignmentsByUser.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());

        Map<UUID, Branch> branchById = allBranchIds.isEmpty()
                ? Map.of()
                : branches.findByRestaurantIdAndIdInOrderByNameAsc(restaurantId, allBranchIds)
                        .stream().collect(Collectors.toMap(Branch::getId, b -> b));

        return managers.stream()
                .map(m -> {
                    List<ManagerResponse.BranchInfo> branchInfos =
                            assignmentsByUser.getOrDefault(m.getId(), List.of()).stream()
                                    .filter(branchById::containsKey)
                                    .map(id -> ManagerResponse.BranchInfo.from(branchById.get(id)))
                                    .toList();
                    return ManagerResponse.from(m, branchInfos);
                })
                .toList();
    }

    @Transactional
    public ManagerResponse create(CreateManagerRequest req) {
        UUID restaurantId = CurrentUser.restaurantId();
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }
        Set<UUID> branchIds = validBranchIds(restaurantId, req.branchIds());

        User manager = new User();
        manager.setName(req.name().trim());
        manager.setEmail(email);
        manager.setPasswordHash(encoder.encode(req.password()));
        manager.setRole(Role.MANAGER);
        manager.setRestaurantId(restaurantId);
        manager = users.save(manager);

        assign(manager.getId(), branchIds);

        List<ManagerResponse.BranchInfo> branchInfos = branchIds.isEmpty()
                ? List.of()
                : branches.findByRestaurantIdAndIdInOrderByNameAsc(restaurantId, branchIds)
                        .stream().map(ManagerResponse.BranchInfo::from).toList();

        return ManagerResponse.from(manager, branchInfos);
    }

    @Transactional
    public ManagerResponse update(UUID id, UpdateManagerRequest req) {
        UUID restaurantId = CurrentUser.restaurantId();
        User manager = users.findByIdAndRestaurantIdAndRole(id, restaurantId, Role.MANAGER)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Manager not found"));
        Set<UUID> branchIds = validBranchIds(restaurantId, req.branchIds());

        manager.setName(req.name().trim());
        if (req.active() != null) manager.setActive(req.active());

        managerBranches.deleteAllForUser(manager.getId());
        assign(manager.getId(), branchIds);

        List<ManagerResponse.BranchInfo> branchInfos = branchIds.isEmpty()
                ? List.of()
                : branches.findByRestaurantIdAndIdInOrderByNameAsc(restaurantId, branchIds)
                        .stream().map(ManagerResponse.BranchInfo::from).toList();

        return ManagerResponse.from(manager, branchInfos);
    }

    @Transactional
    public void resetPassword(UUID id, ResetPasswordRequest req) {
        UUID restaurantId = CurrentUser.restaurantId();
        User manager = users.findByIdAndRestaurantIdAndRole(id, restaurantId, Role.MANAGER)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Manager not found"));
        manager.setPasswordHash(encoder.encode(req.newPassword()));
        // Invalidate all existing refresh tokens so old sessions can't be reused
        refreshTokenRepo.revokeAllForUser(manager.getId());
    }

    @Transactional
    public void delete(UUID id) {
        UUID restaurantId = CurrentUser.restaurantId();
        User manager = users.findByIdAndRestaurantIdAndRole(id, restaurantId, Role.MANAGER)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Manager not found"));
        // Soft-delete: deactivate instead of hard-delete.
        // Their existing short-lived JWT (15 min) may still work until it expires,
        // but login will be rejected on next attempt since AuthService checks is_active.
        manager.setActive(false);
        managerBranches.deleteAllForUser(manager.getId()); // remove branch assignments
    }

    /** Every branch id must belong to the caller's restaurant. */
    private Set<UUID> validBranchIds(UUID restaurantId, List<UUID> ids) {
        Set<UUID> set = new LinkedHashSet<>(ids == null ? List.of() : ids);
        if (!set.isEmpty() && branches.countByRestaurantIdAndIdIn(restaurantId, set) != set.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "One or more branches do not exist in your restaurant");
        }
        return set;
    }

    private void assign(UUID userId, Set<UUID> branchIds) {
        managerBranches.saveAll(branchIds.stream().map(b -> new ManagerBranch(userId, b)).toList());
    }
}
