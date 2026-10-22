package com.aniplato.dao;

import com.aniplato.model.SafetyNotice;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** SQL only. One DAO per table. */
public class SafetyNoticeDao {

    public int insert(Connection c, SafetyNotice notice) throws SQLException {
        String sql = "INSERT INTO safety_notice (offer_id, kind, raised_by, concern, status, raised_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, notice.offerId);
            ps.setString(2, notice.kind.name());
            ps.setObject(3, notice.raisedBy, java.sql.Types.INTEGER);   // null for an overdue-report notice (the system)
            ps.setString(4, notice.concern);
            ps.setString(5, notice.status.name());
            ps.setObject(6, notice.raisedAt);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Creating a safety notice did not return an id");
                return keys.getInt(1);
            }
        }
    }

    public SafetyNotice findById(Connection c, int noticeId) throws SQLException {
        String sql = "SELECT * FROM safety_notice WHERE notice_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, noticeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public int resolve(Connection c, int noticeId, SafetyNotice.Status status, String adminNote,
                       int adminId, LocalDateTime resolvedAt) throws SQLException {
        String sql = "UPDATE safety_notice SET status = ?, admin_note = ?, resolved_by = ?, resolved_at = ? " +
                "WHERE notice_id = ? AND status = 'OPEN'";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, adminNote);
            ps.setInt(3, adminId);
            ps.setObject(4, resolvedAt);
            ps.setInt(5, noticeId);
            return ps.executeUpdate();
        }
    }

    private SafetyNotice map(ResultSet rs) throws SQLException {
        SafetyNotice notice = new SafetyNotice();
        notice.id = rs.getInt("notice_id");
        notice.offerId = rs.getInt("offer_id");
        notice.kind = SafetyNotice.Kind.valueOf(rs.getString("kind"));
        int raisedBy = rs.getInt("raised_by");
        notice.raisedBy = rs.wasNull() ? null : raisedBy;
        notice.concern = rs.getString("concern");
        notice.status = SafetyNotice.Status.valueOf(rs.getString("status"));
        notice.adminNote = rs.getString("admin_note");
        int resolvedBy = rs.getInt("resolved_by");
        notice.resolvedBy = rs.wasNull() ? null : resolvedBy;
        if (rs.getTimestamp("resolved_at") != null) {
            notice.resolvedAt = rs.getTimestamp("resolved_at").toLocalDateTime();
        }
        notice.raisedAt = rs.getTimestamp("raised_at").toLocalDateTime();
        return notice;
    }

    /** A notice with the names the admin's screen shows. The flagged account is the other side of the offer. */
    public record NoticeRow(int id, SafetyNotice.Kind kind, SafetyNotice.Status status, String flaggedName,
                            String raisedByName, String listingText, String concern, String adminNote,
                            LocalDateTime raisedAt) { }

    /** Every notice, newest first, with organization names and the post's description. */
    public List<NoticeRow> findRows(Connection c) throws SQLException {
        String sql = """
                SELECT n.notice_id, n.kind, n.status, n.concern, n.admin_note, n.raised_at,
                       CASE WHEN n.kind = 'OVERDUE_REPORT' THEN
                                 CASE WHEN p.kind = 'LISTING' THEN offerer_reg.name ELSE poster_reg.name END   -- the food group owes the report
                            WHEN n.raised_by = o.offerer_id THEN poster_reg.name
                            ELSE offerer_reg.name END AS flagged_name,
                       CASE WHEN n.raised_by IS NULL THEN 'System'
                            WHEN n.raised_by = o.offerer_id THEN offerer_reg.name
                            ELSE poster_reg.name END AS raised_by_name,
                       COALESCE(fd.description, ud.use_description) AS listing_text
                FROM safety_notice n
                JOIN offer o ON o.offer_id = n.offer_id
                JOIN post p ON p.post_id = o.post_id
                JOIN account offerer ON offerer.account_id = o.offerer_id
                JOIN registration offerer_reg ON offerer_reg.registration_id = offerer.registration_id
                JOIN account poster ON poster.account_id = p.posted_by
                JOIN registration poster_reg ON poster_reg.registration_id = poster.registration_id
                LEFT JOIN food_detail fd ON fd.food_detail_id = p.food_detail_id
                LEFT JOIN use_detail ud ON ud.use_detail_id = p.use_detail_id
                ORDER BY n.raised_at DESC, n.notice_id DESC
                """;
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<NoticeRow> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(new NoticeRow(rs.getInt("notice_id"), SafetyNotice.Kind.valueOf(rs.getString("kind")),
                        SafetyNotice.Status.valueOf(rs.getString("status")), rs.getString("flagged_name"),
                        rs.getString("raised_by_name"), rs.getString("listing_text"), rs.getString("concern"),
                        rs.getString("admin_note"), rs.getTimestamp("raised_at").toLocalDateTime()));
            }
            return rows;
        }
    }

    public int countOpen(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM safety_notice WHERE status = 'OPEN'");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
