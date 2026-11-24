package com.spiceroute.feedback.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_usage_logs")
public class AiUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "restaurant_id", nullable = false, updatable = false)
    private UUID restaurantId;

    @Column(name = "summary_id", updatable = false)
    private UUID summaryId;

    @Column(name = "input_tokens", nullable = false)
    private int inputTokens = 0;

    @Column(name = "output_tokens", nullable = false)
    private int outputTokens = 0;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = Instant.now(); }

    public UUID getId()            { return id; }
    public UUID getRestaurantId()  { return restaurantId; }
    public void setRestaurantId(UUID v) { this.restaurantId = v; }
    public UUID getSummaryId()     { return summaryId; }
    public void setSummaryId(UUID v) { this.summaryId = v; }
    public int getInputTokens()    { return inputTokens; }
    public void setInputTokens(int v) { this.inputTokens = v; }
    public int getOutputTokens()   { return outputTokens; }
    public void setOutputTokens(int v) { this.outputTokens = v; }
    public String getStatus()      { return status; }
    public void setStatus(String v) { this.status = v; }
    public Instant getCreatedAt()  { return createdAt; }
}
