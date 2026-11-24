package com.spiceroute.feedback.table;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class TableController {

    private final TableService service;

    public TableController(TableService service) {
        this.service = service;
    }

    @GetMapping("/branches/{branchId}/tables")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public List<TableResponse> list(@PathVariable UUID branchId) {
        return service.list(branchId);
    }

    @PostMapping("/branches/{branchId}/tables")
    @PreAuthorize("hasRole('OWNER')")
    @ResponseStatus(HttpStatus.CREATED)
    public TableResponse create(@PathVariable UUID branchId, @Valid @RequestBody TableRequest req) {
        return service.create(branchId, req);
    }

    /**
     * Bulk-creates tables and returns both what was created and what was skipped (already existed).
     * HTTP 201 even when some labels are skipped — at least one table was requested.
     */
    @PostMapping("/branches/{branchId}/tables/bulk")
    @PreAuthorize("hasRole('OWNER')")
    @ResponseStatus(HttpStatus.CREATED)
    public BulkTablesResponse createBulk(@PathVariable UUID branchId, @Valid @RequestBody BulkTablesRequest req) {
        return service.createBulk(branchId, req);
    }

    @PutMapping("/tables/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public TableResponse update(@PathVariable UUID id, @Valid @RequestBody TableRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/tables/{id}")
    @PreAuthorize("hasRole('OWNER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
