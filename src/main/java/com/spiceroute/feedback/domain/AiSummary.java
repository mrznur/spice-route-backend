package com.spiceroute.feedback.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "ai_summaries")
public class AiSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "restaurant_id", nullable = false, updatable = false)
    private UUID restaurantId;

    @Column(name = "branch_id", nullable = false, updatable = false)
    private UUID branchId;

    @Column(name = "requested_by", updatable = false)
    private UUID requestedBy;

    @Column(name = "date_from", nullable = false, updatable = false)
    private LocalDate dateFrom;

    @Column(name = "date_to", nullable = false, updatable = false)
    private LocalDate dateTo;

    @Column(name = "input_hash", nullable = false, updatable = false, length = 64)
    private String inputHash;

    @Column(name = "comment_count", nullable = false)
    private int commentCount;

    @Column(columnDefinition = "JSONB")
    private String result;   // stored as JSON string, parsed on read

    @Column(nullable = false, length = 20)
    private String status = "COMPLETED";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = Instant.now(); }

    public UUID getId()           { return id; }
    public UUID getRestaurantId() { return restaurantId; }
    public void setRestaurantId(UUID v) { this.restaurantId = v; }
    public UUID getBranchId()     { return branchId; }
    public void setBranchId(UUID v) { this.branchId = v; }
    public UUID getRequestedBy()  { return requestedBy; }
    public void setRequestedBy(UUID v) { this.requestedBy = v; }
    public LocalDate getDateFrom() { return dateFrom; }
    public void setDateFrom(LocalDate v) { this.dateFrom = v; }
    public LocalDate getDateTo()   { return dateTo; }
    public void setDateTo(LocalDate v) { this.dateTo = v; }
    public String getInputHash()  { return inputHash; }
    public void setInputHash(String v) { this.inputHash = v; }
    public int getCommentCount()  { return commentCount; }
    public void setCommentCount(int v) { this.commentCount = v; }
    public String getResult()     { return result; }
    public void setResult(String v) { this.result = v; }
    public String getStatus()     { return status; }
    public void setStatus(String v) { this.status = v; }
    public Instant getCreatedAt() { return createdAt; }
}
