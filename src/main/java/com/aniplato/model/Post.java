package com.aniplato.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A listing (Route 1, carries a {@link FoodDetail}) or a request (Route 2, carries a {@link UseDetail}).
 * The agreed kg is never stored: it is the accepted offer's kg (A9).
 */
public class Post {
    public enum Kind { LISTING, REQUEST }

    public enum Status {
        OPEN("Open"), ACCEPTED("Accepted"), RESCUED("Rescued"), EXPIRED("Expired"), CANCELLED("Cancelled");

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
    public Kind kind;
    public int posterId;
    public String posterName;
    public String city;
    public double kg;
    public Status status;
    public String cancelReason;
    public LocalDateTime createdAt;
    public FoodDetail food;                          // listings only
    public UseDetail use;                            // requests only
    public List<String> photos = new ArrayList<>();  // stored paths, filled by the service
    public int offerCount;                           // worked out for the poster's own lists, 0 elsewhere
    public Offer acceptedOffer;                      // the Accepted or Completed offer, on the poster's own lists only

    public boolean isListing() {
        return kind == Kind.LISTING;
    }

    /** The time this post stops being useful: the listing's use-by or the request's needed-by (6.6). */
    public LocalDateTime deadline() {
        return isListing() ? food.useBy : use.dueAt;
    }
}
