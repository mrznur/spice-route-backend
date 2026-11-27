
package com.spiceroute.feedback.table;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Tables", description = "Dining table management and bulk QR setup")
@RestController
@RequestMapping("/api")
public class TableController {

    private final TableService service;

    public TableController(TableService service) {
        this.service = service;
    }

    @GetMapping("/branches/{branchId}/tables")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OWNER','MANAGER')")
    public List<TableResponse> list(@PathVariable UUID branchId) {
        return service.list(branchId);
    }

    @PostMapping("/branches/{branchId}/tables")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OWNER')")
    @ResponseStatus(HttpStatus.CREATED)
    public TableResponse create(
            @PathVariable UUID branchId,
            @Valid @RequestBody TableRequest req) {
        return service.create(branchId, req);
    }

    @PostMapping("/branches/{branchId}/tables/bulk")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OWNER')")
    @ResponseStatus(HttpStatus.CREATED)
    public BulkTablesResponse createBulk(
            @PathVariable UUID branchId,
            @Valid @RequestBody BulkTablesRequest req) {
        return service.createBulk(branchId, req);
    }

    @PutMapping("/tables/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OWNER')")
    public TableResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody TableRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/tables/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OWNER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
