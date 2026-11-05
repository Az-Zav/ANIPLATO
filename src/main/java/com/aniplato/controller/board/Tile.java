package com.aniplato.controller.board;

/**
 * One summary tile above a board table: an icon chip and a title, the figure (with an optional unit) and a line
 * under it. Every figure is worked out by a service from what is stored (A9); the tile only shows it.
 */
public record Tile(Icon icon, String title, String value, String unit, String note) {

    /** The icon chip's colour and file, as in the Figma tiles. */
    public enum Icon {
        BLUE("/assets/OpenListings.svg", "board-chip-blue"),
        ORANGE("/assets/AcceptedOffers.svg", "board-chip-orange"),
        GREEN("/assets/KgRescued.svg", "board-chip-green");

        final String file;
        final String style;

        Icon(String file, String style) {
            this.file = file;
            this.style = style;
        }
    }

    /** A tile whose figure has no unit. */
    public static Tile of(Icon icon, String title, String value, String note) {
        return new Tile(icon, title, value, null, note);
    }

    /** A tile whose figure is a weight, shown as "257 kg". */
    public static Tile kg(Icon icon, String title, double kg, String note) {
        String number = kg == Math.rint(kg) ? String.valueOf((long) kg) : String.valueOf(kg);
        return new Tile(icon, title, number, "kg", note);
    }
}
