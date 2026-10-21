package com.aniplato.model;

/**
 * An offer on a post. Only the status is modelled so far; the old fields described the removed listing
 * table, so they were dropped. The full model comes with the offer features (FLOWS.md module 6).
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
}
