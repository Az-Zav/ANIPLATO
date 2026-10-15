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
        String sql = "INSERT INTO safety_notice (offer_id, raised_by, concern, status, raised_at) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, notice.offerId);
            ps.setInt(2, notice.raisedBy);
            ps.setString(3, notice.concern);
            ps.setString(4, notice.status.name());
            ps.setObject(5, notice.raisedAt);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Creating a safety notice did not return an id");
                return keys.getInt(1);
            }
        }
    }

    public List<SafetyNotice> findAll(Connection c) throws SQLException {
        String sql = "SELECT * FROM safety_notice ORDER BY raised_at DESC, notice_id DESC";
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<SafetyNotice> notices = new ArrayList<>();
            while (rs.next()) notices.add(map(rs));
            return notices;
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
        notice.raisedBy = rs.getInt("raised_by");
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
}
