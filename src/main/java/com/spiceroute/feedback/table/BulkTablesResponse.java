package com.spiceroute.feedback.table;

import java.util.List;

/**
 * Returned by the bulk-create endpoint.
 * {@code created} contains the new tables; {@code skipped} lists labels that already existed
 * and were left unchanged. Callers can inspect {@code skipped} to know exactly what was ignored.
 */
public record BulkTablesResponse(List<TableResponse> created, List<String> skipped) {}
