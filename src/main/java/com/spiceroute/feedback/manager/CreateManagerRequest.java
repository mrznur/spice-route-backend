package com.spiceroute.feedback.manager;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateManagerRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72, message = "Password must be 8-72 characters") String password,
        @NotNull List<UUID> branchIds) {}
