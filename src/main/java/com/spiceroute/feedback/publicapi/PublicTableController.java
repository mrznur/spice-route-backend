package com.spiceroute.feedback.publicapi;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** No login required: this is what a customer's phone calls after scanning a QR code. */
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
