package com.aniplato.util;

import com.aniplato.model.Account.Role;

/**
 * Temporary hand-over from register step 1 to step 2 (and back).
 * TODO(service): replaced by a Registration draft held by RegistrationService.
 */
public final class RegistrationDraft {

    private static Role role = Role.DONOR;
    private static String orgTypeLabel = "";

    private RegistrationDraft() {}

    /** DONOR or FOOD_GROUP, as picked on the step 1 toggle. */
    public static Role role() {
        return role;
    }

    public static void setRole(Role value) {
        role = value;
    }

    /** Organization type as shown on step 1; empty if none was picked. */
    public static String orgTypeLabel() {
        return orgTypeLabel;
    }

    public static void setOrgTypeLabel(String value) {
        orgTypeLabel = value == null ? "" : value;
    }
}