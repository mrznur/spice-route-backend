package com.spiceroute.feedback.manager;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** Full replacement of the manager's name, active flag, and branch assignments. */
public record UpdateManagerRequest(
        @NotBlank @Size(max = 150) String name,
        Boolean active,
        @NotNull List<UUID> branchIds) {}
