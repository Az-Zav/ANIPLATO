package com.aniplato.model;

import java.util.List;

/** A charity, parish or similar group that offers to collect food. Details come from its registration. */
public class FoodGroup extends Account {
    @Override
    public String displayName() {
        return registration.name;
    }

    @Override
    public List<ProfileRow> profileRows() {
        return organizationRows("Food group");
    }
}
