package com.aniplato.dao;

import com.aniplato.model.Offer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

/** SQL only. One DAO per table. The rest of the offer queries are added with the offer features (FLOWS.md modules 6 to 8). */
public class OfferDao {

    public Offer.Status findStatus(Connection c, int offerId) throws SQLException {
        String sql = "SELECT status FROM offer WHERE offer_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, offerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Offer.Status.valueOf(rs.getString(1)) : null;
            }
        }
    }

    /** The two sides of an offer: who made it and who posted what it is on. */
    public record Parties(int offererId, int posterId) { }

    public Parties findParties(Connection c, int offerId) throws SQLException {
        String sql = "SELECT o.offerer_id, p.posted_by FROM offer o JOIN post p ON p.post_id = o.post_id WHERE o.offer_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, offerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Parties(rs.getInt(1), rs.getInt(2)) : null;
            }
        }
    }

    /** Completed handoffs received in [from, to). */
    public int countCompleted(Connection c, LocalDateTime from, LocalDateTime to) throws SQLException {
        String sql = "SELECT COUNT(*) FROM offer WHERE status = 'COMPLETED' AND received_at >= ? AND received_at < ?";
        return countBetween(c, sql, from, to);
    }

    /** Offers made in [from, to) that were ever accepted (Accepted, Completed or Lapsed). */
    public int countAccepted(Connection c, LocalDateTime from, LocalDateTime to) throws SQLException {
        String sql = "SELECT COUNT(*) FROM offer WHERE status IN ('ACCEPTED', 'COMPLETED', 'LAPSED') "
                + "AND created_at >= ? AND created_at < ?";
        return countBetween(c, sql, from, to);
    }

    /** Offers made in [from, to) that lapsed after being accepted. */
    public int countLapsed(Connection c, LocalDateTime from, LocalDateTime to) throws SQLException {
        String sql = "SELECT COUNT(*) FROM offer WHERE status = 'LAPSED' AND created_at >= ? AND created_at < ?";
        return countBetween(c, sql, from, to);
    }

    private int countBetween(Connection c, String sql, LocalDateTime from, LocalDateTime to) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, from);
            ps.setObject(2, to);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
