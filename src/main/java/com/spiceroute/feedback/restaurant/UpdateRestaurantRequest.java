package com.spiceroute.feedback.restaurant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateRestaurantRequest(
        @NotBlank @Size(max = 150) String name
) {}
