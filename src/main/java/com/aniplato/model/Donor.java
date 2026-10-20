package com.aniplato.model;

import java.util.List;

/** A Business or Casual donor (told apart by {@link Account#role}) that posts surplus food. Details come from its registration. */
public class Donor extends Account {
    @Override
    public String displayName() {
        return registration.name;
    }

    @Override
    public List<ProfileRow> profileRows() {
        return organizationRows("Donor");
    }
}
