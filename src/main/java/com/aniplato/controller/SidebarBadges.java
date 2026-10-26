package com.aniplato.controller;

import com.aniplato.service.OfferService;
import com.aniplato.service.StatsService;
import javafx.scene.control.Label;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The count badges on the sidebar buttons. A button's "badge-&lt;key&gt;" style class picks its count; a count of zero
 * hides the badge (A9: counts are derived, never stored). Admin: pending accounts and open notices. Donor: offers
 * waiting for a decision. Food group: accepted handoffs waiting for pickup or receipt.
 */
final class SidebarBadges {

    private final StatsService statsService = new StatsService();
    private final OfferService offerService = new OfferService();
    private final List<Label> badges = new ArrayList<>();

    /** Adds the badge for a key (pending, notices, decisions or handoffs) and returns it for the button. */
    Label create(String key) {
        Label badge = new Label();
        badge.getStyleClass().add("sidebar-count-badge");
        badge.setUserData(key);
        badges.add(badge);
        return badge;
    }

    void refresh() {
        for (Label badge : badges) {
            int count = count((String) badge.getUserData());
            badge.setText(String.valueOf(count));
            badge.setVisible(count > 0);
            badge.setManaged(count > 0);
        }
    }

    private int count(String key) {
        try {
            return switch (key) {
                case "pending" -> statsService.getAdminSummary(LocalDate.now()).pendingRegistrations();
                case "notices" -> statsService.getAdminSummary(LocalDate.now()).openSafetyNotices();
                case "decisions" -> offerService.countOffersAwaitingDecision();
                case "handoffs" -> offerService.countAwaitingHandoff();
                default -> 0;
            };
        } catch (RuntimeException e) {
            return 0;   // no database: show no count rather than a wrong one
        }
    }
}
