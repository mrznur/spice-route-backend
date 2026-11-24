package com.spiceroute.feedback.qr;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('OWNER','MANAGER')")
public class QrController {

    private final QrDownloadService service;

    public QrController(QrDownloadService service) {
        this.service = service;
    }

    @GetMapping("/tables/{id}/qr.png")
    public ResponseEntity<byte[]> tablePng(@PathVariable UUID id) {
        return respond(service.tablePng(id), MediaType.IMAGE_PNG);
    }

    @GetMapping("/branches/{branchId}/qr.pdf")
    public ResponseEntity<byte[]> branchPdf(@PathVariable UUID branchId) {
        return respond(service.branchPdf(branchId), MediaType.APPLICATION_PDF);
    }

    private ResponseEntity<byte[]> respond(QrDownloadService.Download d, MediaType type) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(d.filename()).build().toString())
                .contentType(type)
                .body(d.bytes());
    }
}
