package com.aniplato.model;

import java.time.LocalDateTime;
import java.util.Set;

/** What a donor gives: on a listing (Route 1) or on an offer to a request (Route 2). Entered facts only (A9). */
public class FoodDetail {
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

        /** A Casual donor may give raw or sealed-packaged food only (6.5). */
        public boolean allowedForCasual() {
            return this == RAW || this == PACKAGED;
        }
    }

    public enum Storage {
        AMBIENT_DRY("Ambient Dry"), CHILLED("Chilled / Cold-chain"), HOT_HOLD("Hot Hold");

        private final String label;

        Storage(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /** Matches the values of the food_detail.allergens SET column. */
    public enum Allergen {
        GLUTEN("gluten", "Gluten / Wheat"), DAIRY("dairy", "Dairy / Butter"), NUTS("nuts", "Nuts / Peanuts"),
        EGGS("eggs", "Eggs"), HALAL("halal", "Halal Ingredients");

        private final String column;
        private final String label;

        Allergen(String column, String label) {
            this.column = column;
            this.label = label;
        }

        /** The value stored in the SET column. */
        public String column() {
            return column;
        }

        public String label() {
            return label;
        }

        public static Allergen fromColumn(String value) {
            for (Allergen a : values()) if (a.column.equals(value)) return a;
            throw new IllegalArgumentException("Unknown allergen: " + value);
        }
    }

    public int id;
    public String description;
    public Category category;
    public Storage storage;
    public Set<Allergen> allergens = Set.of();
    public LocalDateTime useBy;
    public LocalDateTime pickupStart;
    public LocalDateTime pickupEnd;
    public String handoverInstructions;   // hidden until the offer is accepted (R2.1)
    public LocalDateTime attestedAt;
}
