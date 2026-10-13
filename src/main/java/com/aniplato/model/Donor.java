package com.aniplato.model;

import java.util.List;

/** A business or organization that posts surplus food. Details come from its registration. */
public class Donor extends Account {
    @Override
    public String displayName() {
        return registration.orgName;
    }

    @Override
    public List<ProfileRow> profileRows() {
        return organizationRows("Donor");
    }
}
