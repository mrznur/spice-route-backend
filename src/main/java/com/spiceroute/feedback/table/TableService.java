
package com.spiceroute.feedback.table;

import com.spiceroute.feedback.branch.BranchService;
import com.spiceroute.feedback.common.ApiException;
import com.spiceroute.feedback.domain.Branch;
import com.spiceroute.feedback.domain.DiningTable;
import com.spiceroute.feedback.domain.Role;
import com.spiceroute.feedback.repository.DiningTableRepository;
import com.spiceroute.feedback.security.AuthPrincipal;
import com.spiceroute.feedback.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TableService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final Comparator<DiningTable> ORDER =
            Comparator.comparingInt((DiningTable t) -> t.getLabel().length())
                    .thenComparing(DiningTable::getLabel, String.CASE_INSENSITIVE_ORDER);

    private final DiningTableRepository tables;
    private final BranchService branchService;

    public TableService(DiningTableRepository tables, BranchService branchService) {
        this.tables = tables;
        this.branchService = branchService;
    }

    @Transactional(readOnly = true)
    public List<TableResponse> list(UUID branchId) {
        Branch branch = branchService.requireAccessible(branchId);

        return tables.findByBranchIdAndRestaurantId(
                        branch.getId(), branch.getRestaurantId())
                .stream()
                .sorted(ORDER)
                .map(TableResponse::from)
                .toList();
    }

    @Transactional
    public TableResponse create(UUID branchId, TableRequest req) {
        Branch branch = branchService.requireAccessible(branchId);
        String label = req.label().trim();

        if (tables.existsByBranchIdAndLabel(branch.getId(), label)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "A table with this label already exists in the branch");
        }

        return TableResponse.from(tables.save(newTable(branch, label)));
    }

    @Transactional
    public BulkTablesResponse createBulk(UUID branchId, BulkTablesRequest req) {
        Branch branch = branchService.requireAccessible(branchId);

        Set<String> existing = tables.findByBranchIdAndRestaurantId(
                        branch.getId(), branch.getRestaurantId())
                .stream()
                .map(DiningTable::getLabel)
                .collect(Collectors.toSet());

        String prefix = (req.prefix() == null || req.prefix().isBlank())
                ? "Table" : req.prefix().trim();

        List<DiningTable> toCreate = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        for (int i = 1; i <= req.count(); i++) {
            String label = prefix + " " + i;

            if (existing.contains(label)) {
                skipped.add(label);
            } else {
                toCreate.add(newTable(branch, label));
            }
        }

        List<TableResponse> created = tables.saveAll(toCreate)
                .stream()
                .sorted(ORDER)
                .map(TableResponse::from)
                .toList();

        return new BulkTablesResponse(created, skipped);
    }

    @Transactional
    public TableResponse update(UUID id, TableRequest req) {
        DiningTable table = findAccessibleTable(id);
        String label = req.label().trim();

        if (!label.equals(table.getLabel())
                && tables.existsByBranchIdAndLabel(table.getBranchId(), label)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "A table with this label already exists in the branch");
        }

        table.setLabel(label);

        if (req.active() != null) {
            table.setActive(req.active());
        }

        return TableResponse.from(table);
    }

    @Transactional
    public void delete(UUID id) {
        DiningTable table = findAccessibleTable(id);

        // Soft-delete to preserve historical feedback.
        table.setActive(false);
    }

    private DiningTable findAccessibleTable(UUID id) {
        AuthPrincipal me = CurrentUser.get();

        DiningTable table;

        if (me.role() == Role.SUPER_ADMIN) {
            table = tables.findById(id)
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND, "Table not found"));

            // Also verify that the table's branch exists.
            branchService.requireAccessible(table.getBranchId());
        } else {
            table = tables.findByIdAndRestaurantId(
                            id, CurrentUser.restaurantId())
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND, "Table not found"));

            // Managers must also be assigned to the table's branch.
            branchService.requireAccessible(table.getBranchId());
        }

        return table;
    }

    private DiningTable newTable(Branch branch, String label) {
        DiningTable table = new DiningTable();
        table.setRestaurantId(branch.getRestaurantId());
        table.setBranchId(branch.getId());
        table.setLabel(label);
        table.setQrToken(newToken());
        return table;
    }

    private static String newToken() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
