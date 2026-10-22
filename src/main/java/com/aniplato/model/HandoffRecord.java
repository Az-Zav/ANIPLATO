package com.aniplato.model;

import java.time.LocalDateTime;

/**
 * The record a completed handoff leaves: names, food, agreed kg, times and stated use. It is worked out from the offer
 * and its post each time it is asked for, never stored (A9), and it is not a government certificate.
 */
public record HandoffRecord(String number, int offerId, String donorName, String groupName, String foodDescription,
                            FoodDetail.Category category, double agreedKg, LocalDateTime releasedAt,
                            LocalDateTime receivedAt, String useDescription, String beneficiaries, int headcount,
                            String place, LocalDateTime reportDueAt) {

    /** Printed on every record (Appendix A). */
    public static final String DISCLAIMER =
            "This is an ANIPLATO platform record, not a certificate issued by DSWD, the Philippine Red Cross or any government agency.";
}
