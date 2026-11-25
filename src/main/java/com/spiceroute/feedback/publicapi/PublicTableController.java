package com.spiceroute.feedback.publicapi;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Public", description = "No-auth endpoints for customer QR scanning and feedback")
@RestController
@RequestMapping("/api/public/tables")
public class PublicTableController {

    private final PublicTableService service;

    public PublicTableController(PublicTableService service) {
        this.service = service;
    }

    @GetMapping("/{qrToken}")
    public PublicTableInfo resolve(@PathVariable String qrToken) {
        return service.resolve(qrToken);
    }
}
