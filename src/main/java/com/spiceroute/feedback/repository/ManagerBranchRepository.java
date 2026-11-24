package com.spiceroute.feedback.repository;

import com.spiceroute.feedback.domain.ManagerBranch;
import com.spiceroute.feedback.domain.ManagerBranchId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ManagerBranchRepository extends JpaRepository<ManagerBranch, ManagerBranchId> {
    List<ManagerBranch> findByUserId(UUID userId);
    List<ManagerBranch> findByUserIdIn(Collection<UUID> userIds);
    boolean existsByUserIdAndBranchId(UUID userId, UUID branchId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ManagerBranch mb where mb.userId = :userId")
    void deleteAllForUser(@Param("userId") UUID userId);
}
