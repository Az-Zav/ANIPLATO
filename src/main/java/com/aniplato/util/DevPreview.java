package com.aniplato.util;

import com.aniplato.model.Account;
import com.aniplato.model.Admin;
import com.aniplato.model.City;
import com.aniplato.model.Donor;
import com.aniplato.model.FoodGroup;
import com.aniplato.model.Registration;

import java.time.LocalDateTime;

/**
 * TEMPORARY, for looking at screens without MySQL. Set env var ANIPLATO_PREVIEW to
 * admin, business, casual or foodgroup and Main skips login, starting a fake session.
 * Screens that call services will still fail (no database). Delete this class and its
 * call in Main once MySQL is set up.
 */
public final class DevPreview {
    private DevPreview() {}

    /** The fake logged-in account for ANIPLATO_PREVIEW, or null when the variable is not set. */
    public static Account fromEnvironment() {
        String mode = System.getenv("ANIPLATO_PREVIEW");
        if (mode == null || mode.isBlank()) return null;
        Account account = switch (mode.trim().toLowerCase()) {
            case "admin" -> {
                Admin admin = new Admin();
                admin.role = Account.Role.ADMIN;
                admin.adminName = "Preview Admin";
                yield admin;
            }
            case "business" -> organization(new Donor(), Account.Role.BUSINESS_DONOR, "Preview Bakeshop");
            case "casual" -> organization(new Donor(), Account.Role.CASUAL_DONOR, "Preview Household");
            case "foodgroup" -> organization(new FoodGroup(), Account.Role.FOOD_GROUP, "Preview Food Group");
            default -> throw new IllegalStateException(
                    "ANIPLATO_PREVIEW must be admin, business, casual or foodgroup, not: " + mode);
        };
        account.id = 1;
        account.email = "preview@aniplato.test";
        account.status = Account.Status.ACTIVE;
        account.createdAt = LocalDateTime.now();
        account.passwordChangedAt = LocalDateTime.now();
        return account;
    }

    private static Account organization(Account account, Account.Role role, String name) {
        City city = new City();
        city.id = 1;
        city.name = "Pasig City";
        Registration registration = new Registration();
        registration.role = role;
        registration.name = name;
        registration.city = city;
        registration.contactPerson = "Preview Person";
        account.role = role;
        account.registration = registration;
        return account;
    }
}
