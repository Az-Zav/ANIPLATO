package com.aniplato.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * An offer on a post. An offer on a listing carries a {@link UseDetail}; an offer on a request carries a
 * {@link FoodDetail} (the two routes mirror each other). Release and receipt are stored as times; the agreed kg
 * is this offer's kg once it is accepted (A9).
 */
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

    public int id;
    public int postId;
    public int offererId;
    public String offererName;
    public double kg;
    public Status status;
    public FoodDetail food;                          // offers on a request only
    public UseDetail use;                            // offers on a listing only
    public LocalDateTime releasedAt;                 // the supplier confirmed release
    public LocalDateTime receivedAt;                 // the receiver confirmed receipt
    public LocalDateTime createdAt;
    public List<String> photos = new ArrayList<>();  // stored paths, filled by the service
    public Post post;                                // the post it is on, filled by the service

    /** The time the supplier hands the food over: the offer's pickup time (Route 1) or its window end (Route 2). */
    public LocalDateTime pickupDeadline() {
        return use != null ? use.dueAt : food.pickupEnd;
    }
}
