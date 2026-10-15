package com.aniplato.dao;

import com.aniplato.model.City;
import com.aniplato.model.Listing;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** SQL only. One DAO per table. */
public class ListingDao {

    public Listing findById(Connection c, int listingId) throws SQLException {
        String sql = """
                SELECT l.*, city.name AS city_name
                FROM listing l
                JOIN city ON city.city_id = l.city_id
                WHERE l.listing_id = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, listingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Listing listing = new Listing();
                listing.id = rs.getInt("listing_id");
                listing.postedBy = rs.getInt("posted_by");
                City city = new City();
                city.id = rs.getInt("city_id");
                city.name = rs.getString("city_name");
                listing.city = city;
                listing.foodDescription = rs.getString("food_description");
                listing.category = Listing.Category.valueOf(rs.getString("category"));
                listing.storage = Listing.Storage.valueOf(rs.getString("storage"));
                String allergens = rs.getString("allergens");
                if (allergens != null && !allergens.isBlank()) {
                    for (String allergen : allergens.split(",")) {
                        listing.allergens.add(Listing.Allergen.fromDb(allergen));
                    }
                }
                listing.kg = rs.getBigDecimal("kg");
                listing.useBy = rs.getTimestamp("use_by").toLocalDateTime();
                listing.pickupStart = rs.getTimestamp("pickup_start").toLocalDateTime();
                listing.pickupEnd = rs.getTimestamp("pickup_end").toLocalDateTime();
                listing.handoverInstructions = rs.getString("handover_instructions");
                listing.donorAttestedAt = rs.getTimestamp("donor_attested_at").toLocalDateTime();
                listing.status = Listing.Status.valueOf(rs.getString("status"));
                listing.cancelReason = rs.getString("cancel_reason");
                listing.createdAt = rs.getTimestamp("created_at").toLocalDateTime();
                return listing;
            }
        }
    }

    public List<Integer> findOpenPastUseBy(Connection c, LocalDateTime now) throws SQLException {
        String sql = "SELECT listing_id FROM listing WHERE status = 'OPEN' AND use_by < ? ORDER BY listing_id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, now);
            try (ResultSet rs = ps.executeQuery()) {
                List<Integer> ids = new ArrayList<>();
                while (rs.next()) ids.add(rs.getInt("listing_id"));
                return ids;
            }
        }
    }

    public int expireOpenIfPastUseBy(Connection c, int listingId, LocalDateTime now) throws SQLException {
        String sql = "UPDATE listing SET status = 'EXPIRED' " +
                "WHERE listing_id = ? AND status = 'OPEN' AND use_by < ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, listingId);
            ps.setObject(2, now);
            return ps.executeUpdate();
        }
    }

    public int expireAccepted(Connection c, int listingId) throws SQLException {
        String sql = "UPDATE listing SET status = 'EXPIRED' WHERE listing_id = ? AND status = 'ACCEPTED'";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, listingId);
            return ps.executeUpdate();
        }
    }
}
