package com.aniplato.model;

/** A Metro Manila city. Prints as its name so a ComboBox shows it. */
public class City {
    public int id;
    public String name;

    @Override
    public String toString() {
        return name;
    }

    /** Two City objects are the same city when they have the same id. */
    @Override
    public boolean equals(Object other) {
        return other instanceof City c && c.id == id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
