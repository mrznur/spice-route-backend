package com.spiceroute.feedback.manager;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/managers")
@PreAuthorize("hasRole('OWNER')")
public class ManagerController {

    private final ManagerService service;

    public ManagerController(ManagerService service) {
        this.service = service;
    }

    @GetMapping
    public List<ManagerResponse> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ManagerResponse create(@Valid @RequestBody CreateManagerRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public ManagerResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateManagerRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
