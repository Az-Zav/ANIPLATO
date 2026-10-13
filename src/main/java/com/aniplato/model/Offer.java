package com.aniplato.model;

import com.aniplato.util.Settings;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** One food group's offer on one listing. At most one per group per listing (R3.1). */
public class Offer {
    public enum Status {
        OFFERED("Offered"), ACCEPTED("Accepted"), COMPLETED("Completed"), DECLINED("Declined"),
        WITHDRAWN("Withdrawn"), EXPIRED("Expired"), LAPSED("Lapsed"), CANCELLED("Cancelled");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        /** Text shown on screens. */
        public String label() {
            return label;
        }
    }

    public int id;                          // also the handoff record number
    public int listingId;
    public int groupId;                     // account id of the food group
    public String useDescription;           // what the food is for
    public String beneficiaries;            // who it feeds
    public int headcount;
    public String place;
    public LocalDateTime pickupTime;        // inside the listing's pickup window
    public String message;                  // null if none
    public LocalDateTime groupAttestedAt;
    public Status status;
    public LocalDateTime releasedAt;        // null until the donor confirms release
    public LocalDateTime receivedAt;        // null until the group confirms receipt (R5.1)
    public LocalDateTime createdAt;
    public List<String> photoPaths = new ArrayList<>();   // relative to uploads/, filled by OfferDao

    public boolean isOffered() {
        return status == Status.OFFERED;
    }

    public boolean isReleased() {
        return releasedAt != null;
    }

    public boolean isReceived() {
        return receivedAt != null;
    }

    /** Pickup time plus the grace period. An Accepted offer with no handoff by then becomes Lapsed (6.6). */
    public LocalDateTime lapsesAt() {
        return pickupTime.plusHours(Settings.PICKUP_GRACE_HOURS);
    }

    /** The handoff record number, e.g. HR-2026-12. Worked out, never stored (A9). */
    public String handoffRecordNumber() {
        return "HR-" + createdAt.getYear() + "-" + id;
    }
}
