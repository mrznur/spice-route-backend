package com.spiceroute.feedback.feedback.analytics;

import java.time.LocalDate;

/** One chart point: feedback volume and average rating for a day, week or month (period = bucket start). */
public record TrendPoint(LocalDate period, long count, Double averageRating) {}
