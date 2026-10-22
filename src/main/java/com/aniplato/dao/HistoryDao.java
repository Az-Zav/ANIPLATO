package com.aniplato.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SQL only. The figures behind the dashboards and History, for one account: what it rescued, who it worked with, and
 * every post or offer of its own that has closed. Nothing here is stored; everything is worked out from the offers,
 * posts and the status log (A9).
 */
public class HistoryDao {

    /** Completed handoffs the account was a side of: kg, how many, and with how many different organizations. */
    public record Totals(double kgRescued, int handoffs, int partners) { }

    /**
     * One closed post of the account's own (mine = true) or one closed offer it made (mine = false). For a post,
     * {@code offerStatus} is the status of the offer that decided it (Completed, Lapsed, Cancelled) or null.
     * {@code receiver} says the account was the food group; {@code receivedAt} is set once the food was received.
     */
    public record Row(boolean mine, String title, double kg, String postStatus, String offerStatus, String otherParty,
                      LocalDateTime closedAt, Integer offerId, boolean receiver, LocalDateTime receivedAt) { }

    public Totals totals(Connection c, int accountId) throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(o.kg), 0), COUNT(*),
                       COUNT(DISTINCT CASE WHEN o.offerer_id = ? THEN p.posted_by ELSE o.offerer_id END)
                FROM offer o JOIN post p ON p.post_id = o.post_id
                WHERE o.status = 'COMPLETED' AND (o.offerer_id = ? OR p.posted_by = ?)
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setInt(2, accountId);
            ps.setInt(3, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new Totals(rs.getDouble(1), rs.getInt(2), rs.getInt(3));
            }
        }
    }

    /** Accepted offers the donor still has to release: its own listings with an accepted offer, and its accepted offers on requests. */
    public int countAwaitingRelease(Connection c, int donorId) throws SQLException {
        String sql = """
                SELECT COUNT(*) FROM offer o JOIN post p ON p.post_id = o.post_id
                WHERE o.status = 'ACCEPTED' AND o.released_at IS NULL
                  AND ((p.kind = 'LISTING' AND p.posted_by = ?) OR (p.kind = 'REQUEST' AND o.offerer_id = ?))
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, donorId);
            ps.setInt(2, donorId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Every closed post of the account and every closed offer it made, in no particular order. The time is read from the status log. */
    public List<Row> findClosed(Connection c, int accountId) throws SQLException {
        List<Row> rows = new ArrayList<>();
        String posts = """
                SELECT COALESCE(fd.description, ud.use_description) AS title, COALESCE(o.kg, p.kg) AS kg, p.status AS post_status,
                       o.status AS offer_status, r.name AS other_party, o.offer_id, (p.kind = 'REQUEST') AS receiver, o.received_at,
                       COALESCE((SELECT MAX(s.changed_at) FROM status_change s WHERE s.entity_type = 'POST'
                                 AND s.entity_id = p.post_id AND s.to_status = p.status), p.created_at) AS closed_at
                FROM post p
                LEFT JOIN food_detail fd ON fd.food_detail_id = p.food_detail_id
                LEFT JOIN use_detail ud ON ud.use_detail_id = p.use_detail_id
                LEFT JOIN offer o ON o.post_id = p.post_id AND o.status IN ('COMPLETED', 'LAPSED', 'CANCELLED')
                LEFT JOIN account a ON a.account_id = o.offerer_id
                LEFT JOIN registration r ON r.registration_id = a.registration_id
                WHERE p.posted_by = ? AND p.status IN ('RESCUED', 'EXPIRED', 'CANCELLED')
                """;
        read(c, posts, accountId, true, rows);
        String offers = """
                SELECT COALESCE(ofd.description, pfd.description) AS title, o.kg AS kg, p.status AS post_status,
                       o.status AS offer_status, pr.name AS other_party, o.offer_id, (o.use_detail_id IS NOT NULL) AS receiver, o.received_at,
                       COALESCE((SELECT MAX(s.changed_at) FROM status_change s WHERE s.entity_type = 'OFFER'
                                 AND s.entity_id = o.offer_id AND s.to_status = o.status), o.created_at) AS closed_at
                FROM offer o JOIN post p ON p.post_id = o.post_id
                LEFT JOIN food_detail ofd ON ofd.food_detail_id = o.food_detail_id
                LEFT JOIN food_detail pfd ON pfd.food_detail_id = p.food_detail_id
                JOIN account pa ON pa.account_id = p.posted_by
                JOIN registration pr ON pr.registration_id = pa.registration_id
                WHERE o.offerer_id = ? AND o.status IN ('COMPLETED', 'EXPIRED', 'LAPSED', 'CANCELLED')
                """;
        read(c, offers, accountId, false, rows);
        return rows;
    }

    private void read(Connection c, String sql, int accountId, boolean mine, List<Row> into) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int offerId = rs.getInt("offer_id");
                    Integer offer = rs.wasNull() ? null : offerId;
                    into.add(new Row(mine, rs.getString("title"), rs.getDouble("kg"), rs.getString("post_status"),
                            rs.getString("offer_status"), rs.getString("other_party"), rs.getObject("closed_at", LocalDateTime.class),
                            offer, rs.getBoolean("receiver"), rs.getObject("received_at", LocalDateTime.class)));
                }
            }
        }
    }
}
