package com.spiceroute.feedback.admin;

public record AdminStatsResponse(
        long restaurantCount,
        long userCount,
        long feedbackTotal,
        long feedbackToday
) {}
