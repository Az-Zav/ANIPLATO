package com.aniplato.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** SQL only. The offer table's lookups for the automatic checks (6.6); kept apart from OfferDao to keep both short. */
public class OfferSweepDao {

    /** An Accepted offer whose handoff never happened, with its post. */
    public record MissedPickup(int offerId, int postId) { }

    /**
     * Accepted offers with no release yet (M1) whose pickup time (Route 1) or window end (Route 2) was at or before
     * the cutoff, which is "now minus the grace period".
     */
    public List<MissedPickup> findMissedPickups(Connection c, LocalDateTime cutoff) throws SQLException {
        String sql = "SELECT o.offer_id, o.post_id FROM offer o "
                + "LEFT JOIN use_detail ud ON ud.use_detail_id = o.use_detail_id "
                + "LEFT JOIN food_detail fd ON fd.food_detail_id = o.food_detail_id "
                + "WHERE o.status = 'ACCEPTED' AND o.released_at IS NULL AND COALESCE(ud.due_at, fd.pickup_end) <= ? ORDER BY o.offer_id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, cutoff);
            try (ResultSet rs = ps.executeQuery()) {
                List<MissedPickup> missed = new ArrayList<>();
                while (rs.next()) missed.add(new MissedPickup(rs.getInt(1), rs.getInt(2)));
                return missed;
            }
        }
    }

    /** Offered offers nobody answered by their pickup time (Route 1) or window end (Route 2). */
    public List<Integer> findUnansweredPastPickup(Connection c, LocalDateTime now) throws SQLException {
        String sql = "SELECT o.offer_id FROM offer o "
                + "LEFT JOIN use_detail ud ON ud.use_detail_id = o.use_detail_id "
                + "LEFT JOIN food_detail fd ON fd.food_detail_id = o.food_detail_id "
                + "WHERE o.status = 'OFFERED' AND COALESCE(ud.due_at, fd.pickup_end) < ? ORDER BY o.offer_id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, now);
            try (ResultSet rs = ps.executeQuery()) {
                List<Integer> ids = new ArrayList<>();
                while (rs.next()) ids.add(rs.getInt(1));
                return ids;
            }
        }
    }
}
