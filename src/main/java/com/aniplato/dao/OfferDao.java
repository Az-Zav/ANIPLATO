package com.aniplato.dao;

import com.aniplato.model.Offer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** SQL only. One DAO per table. */
public class OfferDao {

    public Offer findById(Connection c, int offerId) throws SQLException {
        String sql = "SELECT * FROM offer WHERE offer_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, offerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Offer offer = new Offer();
                offer.id = rs.getInt("offer_id");
                offer.listingId = rs.getInt("listing_id");
                offer.groupId = rs.getInt("group_id");
                offer.useDescription = rs.getString("use_description");
                offer.beneficiaries = rs.getString("beneficiaries");
                offer.headcount = rs.getInt("headcount");
                offer.place = rs.getString("place");
                offer.pickupTime = rs.getTimestamp("pickup_time").toLocalDateTime();
                offer.message = rs.getString("message");
                offer.groupAttestedAt = rs.getTimestamp("group_attested_at").toLocalDateTime();
                offer.status = Offer.Status.valueOf(rs.getString("status"));
                if (rs.getTimestamp("released_at") != null) {
                    offer.releasedAt = rs.getTimestamp("released_at").toLocalDateTime();
                }
                if (rs.getTimestamp("received_at") != null) {
                    offer.receivedAt = rs.getTimestamp("received_at").toLocalDateTime();
                }
                offer.createdAt = rs.getTimestamp("created_at").toLocalDateTime();
                return offer;
            }
        }
    }

    public List<Integer> findAcceptedPastGracePeriod(Connection c, LocalDateTime pickupCutoff) throws SQLException {
        String sql = "SELECT offer_id FROM offer WHERE status = 'ACCEPTED' AND pickup_time <= ? ORDER BY offer_id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, pickupCutoff);
            try (ResultSet rs = ps.executeQuery()) {
                List<Integer> ids = new ArrayList<>();
                while (rs.next()) ids.add(rs.getInt("offer_id"));
                return ids;
            }
        }
    }

    public List<Integer> findOfferedPastPickupTime(Connection c, LocalDateTime now) throws SQLException {
        String sql = "SELECT offer_id FROM offer WHERE status = 'OFFERED' AND pickup_time < ? ORDER BY offer_id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, now);
            try (ResultSet rs = ps.executeQuery()) {
                List<Integer> ids = new ArrayList<>();
                while (rs.next()) ids.add(rs.getInt("offer_id"));
                return ids;
            }
        }
    }

    public Integer findListingId(Connection c, int offerId) throws SQLException {
        String sql = "SELECT listing_id FROM offer WHERE offer_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, offerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("listing_id") : null;
            }
        }
    }

    public int lapseAcceptedIfPastGracePeriod(Connection c, int offerId, LocalDateTime pickupCutoff)
            throws SQLException {
        String sql = "UPDATE offer SET status = 'LAPSED' " +
                "WHERE offer_id = ? AND status = 'ACCEPTED' AND pickup_time <= ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, offerId);
            ps.setObject(2, pickupCutoff);
            return ps.executeUpdate();
        }
    }

    public int expireOfferedIfPastPickupTime(Connection c, int offerId, LocalDateTime now) throws SQLException {
        String sql = "UPDATE offer SET status = 'EXPIRED' " +
                "WHERE offer_id = ? AND status = 'OFFERED' AND pickup_time < ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, offerId);
            ps.setObject(2, now);
            return ps.executeUpdate();
        }
    }
}
