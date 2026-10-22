package com.aniplato.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

/** SQL only. The offer table's counts for the dashboard and the monthly report; kept apart from OfferDao to keep both short. */
public class OfferStatsDao {

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
