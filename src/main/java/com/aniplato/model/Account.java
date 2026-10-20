package com.aniplato.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Login. Subclasses supply the profile rows shown on the Profile screen, so the shared screen
 * never checks the user's role (A5). Organization details live in {@link Registration}.
 */
public abstract class Account {
    public enum Role {
        ADMIN("Admin", "admin"),
        BUSINESS_DONOR("Business Donor", "donor"),
        CASUAL_DONOR("Casual Donor", "donor"),
        FOOD_GROUP("Food Group", "foodgroup");

        private final String displayName;
        private final String key;

        Role(String displayName, String key) {
            this.displayName = displayName;
            this.key = key;
        }

        /** Text for the shell header. */
        public String displayName() {
            return displayName;
        }

        /** Matches the "role-<key>" style class on shell sidebar buttons; both donor roles share "donor". */
        public String key() {
            return key;
        }
    }
    public enum Status { ACTIVE, SUSPENDED }

    /** One label and value line on the Profile screen. */
    public record ProfileRow(String label, String value) { }

    /** How dates appear in profile rows, e.g. "Oct 13, 2026". */
    private static final DateTimeFormatter PROFILE_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy");

    // The password hash is deliberately not a field here: only the DAO and
    // AuthenticationService handle it, so it never reaches a screen.
    public int id;
    public String email;                    // the login, unique
    public Role role;
    public Status status;
    public String suspendReason;            // null unless suspended; shown to the organization
    public boolean mustChangePassword;      // true until the temporary password is replaced (R1.7)
    public LocalDateTime passwordChangedAt; // profile: "Last changed"
    public LocalDateTime createdAt;         // admin's "Since" date
    public Registration registration;       // organization details; null for admin, filled by the DAO

    //---STATUS AND ACCESS---

    public boolean isSuspended() {
        return status == Status.SUSPENDED;
    }

    /**
     * A Suspended account cannot post, re-post, send an offer or accept an offer (6.3, R2.5, R4.4).
     * Everything else (finishing handoffs, flagging, renewals, profile) still works.
     */
    public boolean canPostOrOffer() {
        return status == Status.ACTIVE;
    }

    //---SUBCLASS HOOKS---

    /** Name shown in headers and lists: the organization name, or the admin's name. */
    public abstract String displayName();

    /** The rows the Profile screen lists for this kind of account. */
    public abstract List<ProfileRow> profileRows();

    //---SHARED HELPERS---

    /**
     * Rows common to donors and food groups: organization, type, city, contact person and email.
     * Phone is left out on purpose; it comes from one service method only (A7).
     */
    protected List<ProfileRow> organizationRows(String organizationLabel) {
        List<ProfileRow> rows = new ArrayList<>();
        rows.add(new ProfileRow(organizationLabel, registration.name));
        rows.add(new ProfileRow("City", registration.city.name));
        rows.add(new ProfileRow("Contact person", registration.contactPerson));
        rows.add(new ProfileRow("Email", email));
        rows.add(lastChangedRow());
        return rows;
    }

    /** "Last changed" row for the password, shared by every account type. */
    protected ProfileRow lastChangedRow() {
        return new ProfileRow("Last changed", passwordChangedAt.format(PROFILE_DATE));
    }

    /** Date formatted for profile rows, for subclasses that add their own. */
    protected static String formatDate(LocalDateTime when) {
        return when.format(PROFILE_DATE);
    }
}
