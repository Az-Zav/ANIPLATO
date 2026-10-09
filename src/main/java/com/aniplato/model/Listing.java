package com.aniplato.model;

public class Listing {
    public enum Status { OPEN, ACCEPTED, RESCUED, EXPIRED, CANCELLED }
    public enum Category { RAW, COOKED, BAKED, PACKAGED }
    public enum Storage { AMBIENT_DRY, CHILLED, HOT_HOLD }
    public enum Allergen { GLUTEN, DAIRY, NUTS, EGGS, HALAL }
}
