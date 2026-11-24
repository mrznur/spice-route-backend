package com.spiceroute.feedback.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpdateRestaurantStatusRequest(
        @NotNull
        @Pattern(regexp = "ACTIVE|SUSPENDED", message = "status must be ACTIVE or SUSPENDED")
        String status
) {}
