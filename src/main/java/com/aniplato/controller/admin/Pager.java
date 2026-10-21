package com.aniplato.controller.admin;

import java.util.List;

/**
 * Page arithmetic for the admin lists (Previous / Next and "Showing 1–10 of 25"). The page is kept inside the
 * valid range whenever the list changes, so a list that shrinks never leaves the screen on a page that is gone.
 */
final class Pager<T> {

    private final int pageSize;
    private List<T> items = List.of();
    private int page = 1;

    Pager(int pageSize) {
        this.pageSize = pageSize;
    }

    /** New data (for example after a reload). The current page is kept when it still exists, or moved back to the last one. */
    void setItems(List<T> items) {
        this.items = items;
        page = Math.min(page, totalPages());
    }

    /** The rows to show on the current page. */
    List<T> current() {
        return items.subList(firstIndex(), lastIndex());
    }

    int total() {
        return items.size();
    }

    int totalPages() {
        return Math.max(1, (int) Math.ceil((double) items.size() / pageSize));
    }

    boolean hasPrevious() {
        return page > 1;
    }

    boolean hasNext() {
        return page < totalPages();
    }

    void previous() {
        if (hasPrevious()) page--;
    }

    void next() {
        if (hasNext()) page++;
    }

    /** "Showing 11–20 of 25", or "Showing 0 of 0" for an empty list. */
    String info() {
        return items.isEmpty() ? "Showing 0 of 0" : "Showing " + (firstIndex() + 1) + "–" + lastIndex() + " of " + items.size();
    }

    private int firstIndex() {
        return (page - 1) * pageSize;
    }

    private int lastIndex() {
        return Math.min(page * pageSize, items.size());
    }
}
