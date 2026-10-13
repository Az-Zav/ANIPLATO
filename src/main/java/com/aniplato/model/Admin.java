package com.aniplato.model;

import java.util.List;

/** The platform admin. Has no registration; the name is stored on the account itself. */
public class Admin extends Account {
    public String adminName;

    @Override
    public String displayName() {
        return adminName;
    }

    @Override
    public List<ProfileRow> profileRows() {
        return List.of(
                new ProfileRow("Name", adminName),
                new ProfileRow("Email", email),
                new ProfileRow("Since", formatDate(createdAt)),
                lastChangedRow());
    }
}
