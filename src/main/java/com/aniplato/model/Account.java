package com.aniplato.model;

/** Login. Subclasses supply the profile rows shown on the Profile screen. */
public abstract class Account {
    public enum Role { ADMIN, DONOR, FOOD_GROUP }
    public enum Status { ACTIVE, SUSPENDED }
    public enum OrgType { RESTAURANT, BAKERY, CANTEEN, CATERER, GROCERY, PARISH, SCHOOL, ORPHANAGE, CHARITY }
}
