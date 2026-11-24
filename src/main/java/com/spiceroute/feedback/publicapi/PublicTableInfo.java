package com.spiceroute.feedback.publicapi;

/** What the customer-facing form needs to show. Deliberately no internal ids. */
public record PublicTableInfo(String restaurantName, String branchName, String tableLabel) {}
