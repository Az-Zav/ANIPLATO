package com.aniplato.dao;

import com.aniplato.model.Report;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** SQL only. One DAO per table: saving and reading a report, the overdue lookup, and the report numbers for stats. */
public class ReportDao {

    public int insert(Connection c, Report r) throws SQLException {
        String sql = "INSERT INTO report (offer_id, event_name, event_date, people_served, description, photo_consent_at, submitted_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.offerId);
            ps.setString(2, r.eventName);
            ps.setObject(3, r.eventDate);
            ps.setInt(4, r.peopleServed);
            ps.setString(5, r.description);
            ps.setObject(6, r.photoConsentAt);
            ps.setObject(7, r.submittedAt);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    /** The report of one handoff, or null. */
    public Report findByOffer(Connection c, int offerId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM report WHERE offer_id = ?")) {
            ps.setInt(1, offerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Report r = new Report();
                r.id = rs.getInt("report_id");
                r.offerId = rs.getInt("offer_id");
                r.eventName = rs.getString("event_name");
                r.eventDate = rs.getObject("event_date", LocalDate.class);
                r.peopleServed = rs.getInt("people_served");
                r.description = rs.getString("description");
                r.photoConsentAt = rs.getObject("photo_consent_at", LocalDateTime.class);
                r.submittedAt = rs.getObject("submitted_at", LocalDateTime.class);
                return r;
            }
        }
    }

    /** A completed handoff whose report the food group has not submitted yet. */
    public record Pending(int offerId, String title, LocalDateTime receivedAt) { }

    /** The account's own unreported handoffs, oldest receipt first: a food group's "reports due" list. */
    public List<Pending> findPending(Connection c, int receiverId) throws SQLException {
        String sql = """
                SELECT o.offer_id, COALESCE(pfd.description, pud.use_description) AS title, o.received_at
                FROM offer o JOIN post p ON p.post_id = o.post_id
                LEFT JOIN food_detail pfd ON pfd.food_detail_id = p.food_detail_id
                LEFT JOIN use_detail pud ON pud.use_detail_id = p.use_detail_id
                WHERE o.status = 'COMPLETED'
                  AND ((p.kind = 'LISTING' AND o.offerer_id = ?) OR (p.kind = 'REQUEST' AND p.posted_by = ?))
                  AND NOT EXISTS (SELECT 1 FROM report r WHERE r.offer_id = o.offer_id)
                ORDER BY o.received_at, o.offer_id
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, receiverId);
            ps.setInt(2, receiverId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Pending> pending = new ArrayList<>();
                while (rs.next()) pending.add(new Pending(rs.getInt(1), rs.getString(2), rs.getObject(3, LocalDateTime.class)));
                return pending;
            }
        }
    }

    /** Completed handoffs past their report deadline with no report and no overdue flag yet (the flag is raised once per handoff). */
    public List<Integer> findOverdueWithoutFlag(Connection c, int deadlineDays, LocalDateTime now) throws SQLException {
        String sql = """
                SELECT o.offer_id FROM offer o
                WHERE o.status = 'COMPLETED' AND DATE_ADD(o.received_at, INTERVAL ? DAY) < ?
                  AND NOT EXISTS (SELECT 1 FROM report r WHERE r.offer_id = o.offer_id)
                  AND NOT EXISTS (SELECT 1 FROM safety_notice n WHERE n.offer_id = o.offer_id AND n.kind = 'OVERDUE_REPORT')
                ORDER BY o.offer_id
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, deadlineDays);
            ps.setObject(2, now);
            try (ResultSet rs = ps.executeQuery()) {
                List<Integer> ids = new ArrayList<>();
                while (rs.next()) ids.add(rs.getInt(1));
                return ids;
            }
        }
    }

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
