package com.aniplato.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

/** SQL only. One DAO per table. */
public class ReportDao {

    /** People served in reports of handoffs received in [from, to). */
    public int sumPeopleServed(Connection c, LocalDateTime from, LocalDateTime to) throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(r.people_served), 0)
                FROM report r
                JOIN offer o ON o.offer_id = r.offer_id
                WHERE o.received_at >= ? AND o.received_at < ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, from);
            ps.setObject(2, to);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Completed handoffs received in [from, to) whose report deadline has passed. */
    public int countHandoffsPastDeadline(Connection c, LocalDateTime from, LocalDateTime to, int deadlineDays,
                                         LocalDateTime now) throws SQLException {
        return countHandoffs(c, from, to, deadlineDays, now, false);
    }

    /** Of those, the ones whose report came in by the deadline. */
    public int countReportedOnTime(Connection c, LocalDateTime from, LocalDateTime to, int deadlineDays,
                                   LocalDateTime now) throws SQLException {
        return countHandoffs(c, from, to, deadlineDays, now, true);
    }

    private int countHandoffs(Connection c, LocalDateTime from, LocalDateTime to, int deadlineDays, LocalDateTime now,
                              boolean onTimeOnly) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM offer o
                LEFT JOIN report r ON r.offer_id = o.offer_id
                WHERE o.status = 'COMPLETED' AND o.received_at >= ? AND o.received_at < ?
                  AND DATE_ADD(o.received_at, INTERVAL ? DAY) < ?
                """ + (onTimeOnly ? " AND r.submitted_at <= DATE_ADD(o.received_at, INTERVAL ? DAY)" : "");
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, from);
            ps.setObject(2, to);
            ps.setInt(3, deadlineDays);
            ps.setObject(4, now);
            if (onTimeOnly) ps.setInt(5, deadlineDays);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
