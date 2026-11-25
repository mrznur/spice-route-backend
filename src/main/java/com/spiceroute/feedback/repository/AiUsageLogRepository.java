package com.spiceroute.feedback.repository;

import com.spiceroute.feedback.domain.AiUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, UUID> {}
