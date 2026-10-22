package com.aniplato.dao;

import com.aniplato.model.Offer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** SQL only. One DAO per table: sending, withdrawing and listing offers, status changes, and the counts for stats and safety. */
public class OfferDao {

    private static final String OFFER_SELECT = """
            SELECT o.offer_id, o.post_id, o.offerer_id, o.kg, o.status, o.released_at, o.received_at, o.created_at,
                   r.name AS offerer_name,
            """ + DetailDao.FOOD_COLUMNS + ",\n" + DetailDao.USE_COLUMNS + """

            FROM offer o
            JOIN account a ON a.account_id = o.offerer_id
            JOIN registration r ON r.registration_id = a.registration_id
            LEFT JOIN food_detail fd ON fd.food_detail_id = o.food_detail_id
            LEFT JOIN use_detail ud ON ud.use_detail_id = o.use_detail_id
            """;

    private final DetailDao detailDao = new DetailDao();

    //---SEND AND WITHDRAW---

    /** Saves a new offer for a detail already inserted; exactly one of the two detail ids is set. */
    public int insert(Connection c, int postId, int offererId, double kg, Integer foodDetailId, Integer useDetailId,
                      LocalDateTime createdAt) throws SQLException {
        String sql = "INSERT INTO offer (post_id, offerer_id, kg, food_detail_id, use_detail_id, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, postId);
            ps.setInt(2, offererId);
            ps.setDouble(3, kg);
            ps.setObject(4, foodDetailId, Types.INTEGER);
            ps.setObject(5, useDetailId, Types.INTEGER);
            ps.setObject(6, createdAt);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    /** Puts a Withdrawn offer back to Offered with new details (the table allows one offer per offerer per post). Returns rows changed. */
    public int reopenIfWithdrawn(Connection c, int offerId, double kg, Integer foodDetailId, Integer useDetailId,
                                 LocalDateTime createdAt) throws SQLException {
        String sql = "UPDATE offer SET status = 'OFFERED', kg = ?, food_detail_id = ?, use_detail_id = ?, created_at = ? "
                + "WHERE offer_id = ? AND status = 'WITHDRAWN'";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDouble(1, kg);
            ps.setObject(2, foodDetailId, Types.INTEGER);
            ps.setObject(3, useDetailId, Types.INTEGER);
            ps.setObject(4, createdAt);
            ps.setInt(5, offerId);
            return ps.executeUpdate();
        }
    }

    /** Changes the status only if the offer is still in the expected one, so two people acting at once cannot both win. Returns rows changed. */
    public int updateStatusIf(Connection c, int offerId, Offer.Status from, Offer.Status to) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE offer SET status = ? WHERE offer_id = ? AND status = ?")) {
            ps.setString(1, to.name());
            ps.setInt(2, offerId);
            ps.setString(3, from.name());
            return ps.executeUpdate();
        }
    }

    //---READ---

    public Offer findById(Connection c, int offerId) throws SQLException {
        List<Offer> found = query(c, OFFER_SELECT + "WHERE o.offer_id = ?", offerId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The offer one account already made on a post, in any status, or null. */
    public Offer findByPostAndOfferer(Connection c, int postId, int offererId) throws SQLException {
        List<Offer> found = query(c, OFFER_SELECT + "WHERE o.post_id = ? AND o.offerer_id = ?", postId, offererId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Every offer one account made, newest first. */
    public List<Offer> findByOfferer(Connection c, int offererId) throws SQLException {
        return query(c, OFFER_SELECT + "WHERE o.offerer_id = ? ORDER BY o.created_at DESC, o.offer_id DESC", offererId);
    }

    private List<Offer> query(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            try (ResultSet rs = ps.executeQuery()) {
                List<Offer> offers = new ArrayList<>();
                while (rs.next()) offers.add(read(rs));
                return offers;
            }
        }
    }

    private Offer read(ResultSet rs) throws SQLException {
        Offer o = new Offer();
        o.id = rs.getInt("offer_id");
        o.postId = rs.getInt("post_id");
        o.offererId = rs.getInt("offerer_id");
        o.offererName = rs.getString("offerer_name");
        o.kg = rs.getDouble("kg");
        o.status = Offer.Status.valueOf(rs.getString("status"));
        o.releasedAt = rs.getObject("released_at", LocalDateTime.class);
        o.receivedAt = rs.getObject("received_at", LocalDateTime.class);
        o.createdAt = rs.getObject("created_at", LocalDateTime.class);
        o.food = detailDao.readFood(rs);
        o.use = detailDao.readUse(rs);
        return o;
    }

    //---STATUS AND PARTIES---

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
