package com.aniplato.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/** One posted surplus. A food group re-posting extra food also creates a Listing (R2.6). */
public class Listing {
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

    public enum Category {
        RAW("Raw"), COOKED("Cooked"), BAKED("Baked"), PACKAGED("Packaged");

        private final String label;

        Category(String label) {
            this.label = label;
        }

        /** Text shown on screens. */
        public String label() {
            return label;
        }
    }

    /** Storage condition (R2.8). */
    public enum Storage {
        AMBIENT_DRY("Ambient Dry"), CHILLED("Chilled Cold-Chain"), HOT_HOLD("Hot Hold");

        private final String label;

        Storage(String label) {
            this.label = label;
        }

        /** Text shown on screens. */
        public String label() {
            return label;
        }
    }

    /** Optional allergen labels, for information only (R2.7). */
    public enum Allergen {
        GLUTEN("Gluten / Wheat"), DAIRY("Dairy / Butter"), NUTS("Nuts / Peanuts"),
        EGGS("Eggs"), HALAL("Halal Ingredients");

        private final String label;

        Allergen(String label) {
            this.label = label;
        }

        /** Text shown on screens. */
        public String label() {
            return label;
        }

        /** Value stored in the database SET column, which is lowercase: GLUTEN becomes gluten. */
        public String dbValue() {
            return name().toLowerCase();
        }

        /** Reads a database value back into the enum. */
        public static Allergen fromDb(String value) {
            return valueOf(value.toUpperCase());
        }
    }

    public int id;
    public int postedBy;                    // account id of the donor, or of a food group re-posting
    public City city;                       // copied from the poster at posting
    public String foodDescription;
    public Category category;
    public Storage storage;
    public Set<Allergen> allergens = EnumSet.noneOf(Allergen.class);   // empty means none labeled
    public BigDecimal kg;                   // must be above 0 (R2.1)
    public LocalDateTime useBy;             // must be in the future at posting (R2.2)
    public LocalDateTime pickupStart;
    public LocalDateTime pickupEnd;         // must be before useBy (R2.2)
    public String handoverInstructions;     // null if none; hidden until an offer is accepted (R2.4)
    public LocalDateTime donorAttestedAt;   // the attestation tick (R2.3)
    public Status status;
    public String cancelReason;             // null unless cancelled (R6.1)
    public LocalDateTime createdAt;

    public boolean isOpen() {
        return status == Status.OPEN;
    }

    /** True once the use-by time has passed. Maintenance uses this to expire Open listings. */
    public boolean isPastUseBy(LocalDateTime now) {
        return now.isAfter(useBy);
    }

    /** True if the given account posted this listing. Lets screens tell "my" listings apart without a role check. */
    public boolean isPostedBy(int accountId) {
        return postedBy == accountId;
    }
}
