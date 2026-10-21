package com.aniplato.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** SQL only. One DAO per table. Holds the post totals for dashboards and reports; listing and request queries join it later. */
public class PostDao {

    /** One city's kg for a month. "Closed" means Rescued or Expired; cancelled posts stay out of the rate. Nothing here is stored (A9). */
    public record CityTotals(String city, double kgPosted, double kgRescued, double kgExpired, double kgClosed) { }

    /** Posts created in [from, to), by city, biggest first. kgRescued is the kg of completed offers on those posts. */
    public List<CityTotals> totalsByCity(Connection c, LocalDateTime from, LocalDateTime to) throws SQLException {
        String sql = """
                SELECT city.name,
                       COALESCE(SUM(p.kg), 0) AS posted,
                       COALESCE(SUM(CASE WHEN p.status = 'EXPIRED' THEN p.kg END), 0) AS expired,
                       COALESCE(SUM(CASE WHEN p.status IN ('RESCUED', 'EXPIRED') THEN p.kg END), 0) AS closed,
                       COALESCE(SUM((SELECT COALESCE(SUM(o.kg), 0) FROM offer o
                                     WHERE o.post_id = p.post_id AND o.status = 'COMPLETED')), 0) AS rescued
                FROM post p
                JOIN city ON city.city_id = p.city_id
                WHERE p.created_at >= ? AND p.created_at < ?
                GROUP BY city.city_id, city.name
                ORDER BY posted DESC, city.name
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, from);
            ps.setObject(2, to);
            try (ResultSet rs = ps.executeQuery()) {
                List<CityTotals> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(new CityTotals(rs.getString(1), rs.getDouble("posted"), rs.getDouble("rescued"),
                            rs.getDouble("expired"), rs.getDouble("closed")));
                }
                return rows;
            }
        }
    }

    /** kg on listings (not requests) created in [from, to). */
    public double sumKgListed(Connection c, LocalDateTime from, LocalDateTime to) throws SQLException {
        String sql = "SELECT COALESCE(SUM(kg), 0) FROM post WHERE kind = 'LISTING' AND created_at >= ? AND created_at < ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, from);
            ps.setObject(2, to);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble(1);
            }
        }
    }

    /** Posts created in [from, to) that got at least one offer. */
    public int countWithOffers(Connection c, LocalDateTime from, LocalDateTime to) throws SQLException {
        return count(c, from, to, "");
    }

    /** Posts created in [from, to) that had an offer accepted (Accepted, Completed or Lapsed offers). */
    public int countWithAcceptedOffer(Connection c, LocalDateTime from, LocalDateTime to) throws SQLException {
        return count(c, from, to, "AND o.status IN ('ACCEPTED', 'COMPLETED', 'LAPSED')");
    }

    private int count(Connection c, LocalDateTime from, LocalDateTime to, String offerFilter) throws SQLException {
        String sql = "SELECT COUNT(*) FROM post p WHERE p.created_at >= ? AND p.created_at < ? "
                + "AND EXISTS (SELECT 1 FROM offer o WHERE o.post_id = p.post_id " + offerFilter + ")";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, from);
            ps.setObject(2, to);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Every month that has at least one post, newest first. */
    public List<YearMonth> findMonthsWithPosts(Connection c) throws SQLException {
        String sql = "SELECT DISTINCT DATE_FORMAT(created_at, '%Y-%m') AS ym FROM post ORDER BY ym DESC";
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<YearMonth> months = new ArrayList<>();
            while (rs.next()) months.add(YearMonth.parse(rs.getString("ym")));
            return months;
        }
    }
}
