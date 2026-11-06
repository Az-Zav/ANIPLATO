package com.aniplato.controller;

import com.aniplato.service.AcceptanceService;
import com.aniplato.service.HandoffService;
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
    private final AcceptanceService acceptanceService = new AcceptanceService();
    private final HandoffService handoffService = new HandoffService();
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
        StatsService.AdminSummary admin = null;   // one summary feeds both admin badges
        for (Label badge : badges) {
            String key = (String) badge.getUserData();
            if (admin == null && ("pending".equals(key) || "notices".equals(key))) admin = adminSummary();
            int count = count(key, admin);
            badge.setText(String.valueOf(count));
            badge.setVisible(count > 0);
            badge.setManaged(count > 0);
        }
    }

    private StatsService.AdminSummary adminSummary() {
        try {
            return statsService.getAdminSummary(LocalDate.now());
        } catch (RuntimeException e) {
            return null;   // no database: show no count rather than a wrong one
        }
    }

    private int count(String key, StatsService.AdminSummary admin) {
        try {
            return switch (key) {
                case "pending" -> admin == null ? 0 : admin.pendingRegistrations();
                case "notices" -> admin == null ? 0 : admin.openSafetyNotices();
                case "decisions" -> acceptanceService.countOffersAwaitingDecision();
                case "handoffs" -> handoffService.countAwaitingHandoff();
                default -> 0;
            };
        } catch (RuntimeException e) {
            return 0;   // no database: show no count rather than a wrong one
        }
    }
}
