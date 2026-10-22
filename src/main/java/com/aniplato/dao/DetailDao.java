package com.aniplato.dao;

import com.aniplato.model.FoodDetail;
import com.aniplato.model.UseDetail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.StringJoiner;

/**
 * SQL only. food_detail and use_detail: the two halves a post and its offers are made of. Reads happen in the
 * Post and Offer queries, which join these tables and call {@link #readFood} and {@link #readUse}.
 */
public class DetailDao {

    /** Columns the joined queries select, aliased fd_* and ud_* so both can sit in one row. */
    public static final String FOOD_COLUMNS = """
            fd.food_detail_id AS fd_id, fd.description AS fd_description, fd.category AS fd_category,
            fd.storage AS fd_storage, fd.allergens AS fd_allergens, fd.use_by AS fd_use_by,
            fd.pickup_start AS fd_pickup_start, fd.pickup_end AS fd_pickup_end,
            fd.handover_instructions AS fd_handover, fd.attested_at AS fd_attested_at""";
    public static final String USE_COLUMNS = """
            ud.use_detail_id AS ud_id, ud.use_description AS ud_description, ud.event_date AS ud_event_date,
            ud.beneficiaries AS ud_beneficiaries, ud.headcount AS ud_headcount, ud.place AS ud_place,
            ud.due_at AS ud_due_at, ud.message AS ud_message, ud.accepted_categories AS ud_categories,
            ud.attested_at AS ud_attested_at""";

    public int insertFood(Connection c, FoodDetail d) throws SQLException {
        String sql = "INSERT INTO food_detail (description, category, storage, allergens, use_by, pickup_start, "
                + "pickup_end, handover_instructions, attested_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, d.description);
            ps.setString(2, d.category.name());
            ps.setString(3, d.storage.name());
            if (d.allergens.isEmpty()) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                StringJoiner allergens = new StringJoiner(",");
                d.allergens.forEach(a -> allergens.add(a.column()));
                ps.setString(4, allergens.toString());
            }
            ps.setObject(5, d.useBy);
            ps.setObject(6, d.pickupStart);
            ps.setObject(7, d.pickupEnd);
            ps.setString(8, d.handoverInstructions);
            ps.setObject(9, d.attestedAt);
            ps.executeUpdate();
            return generatedId(ps);
        }
    }

    public int insertUse(Connection c, UseDetail d) throws SQLException {
        String sql = "INSERT INTO use_detail (use_description, event_date, beneficiaries, headcount, place, due_at, "
                + "message, accepted_categories, attested_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, d.useDescription);
            ps.setObject(2, d.eventDate);
            ps.setString(3, d.beneficiaries);
            ps.setInt(4, d.headcount);
            ps.setString(5, d.place);
            ps.setObject(6, d.dueAt);
            ps.setString(7, d.message);
            if (d.acceptedCategories.isEmpty()) {
                ps.setNull(8, Types.VARCHAR);
            } else {
                StringJoiner categories = new StringJoiner(",");
                d.acceptedCategories.forEach(cat -> categories.add(cat.name()));
                ps.setString(8, categories.toString());
            }
            ps.setObject(9, d.attestedAt);
            ps.executeUpdate();
            return generatedId(ps);
        }
    }

    /** Removes a food detail nothing points at any more (an offer re-opened with new details). */
    public void deleteFood(Connection c, int foodDetailId) throws SQLException {
        delete(c, "DELETE FROM food_detail WHERE food_detail_id = ?", foodDetailId);
    }

    public void deleteUse(Connection c, int useDetailId) throws SQLException {
        delete(c, "DELETE FROM use_detail WHERE use_detail_id = ?", useDetailId);
    }

    private static void delete(Connection c, String sql, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Reads the fd_* columns of the current row; null when the row has no food detail (the LEFT JOIN found none). */
    public FoodDetail readFood(ResultSet rs) throws SQLException {
        int id = rs.getInt("fd_id");
        if (rs.wasNull()) return null;
        FoodDetail d = new FoodDetail();
        d.id = id;
        d.description = rs.getString("fd_description");
        d.category = FoodDetail.Category.valueOf(rs.getString("fd_category"));
        d.storage = FoodDetail.Storage.valueOf(rs.getString("fd_storage"));
        Set<FoodDetail.Allergen> allergens = EnumSet.noneOf(FoodDetail.Allergen.class);
        String stored = rs.getString("fd_allergens");
        if (stored != null && !stored.isEmpty()) {
            for (String value : stored.split(",")) allergens.add(FoodDetail.Allergen.fromColumn(value));
        }
        d.allergens = allergens;
        d.useBy = rs.getObject("fd_use_by", LocalDateTime.class);
        d.pickupStart = rs.getObject("fd_pickup_start", LocalDateTime.class);
        d.pickupEnd = rs.getObject("fd_pickup_end", LocalDateTime.class);
        d.handoverInstructions = rs.getString("fd_handover");
        d.attestedAt = rs.getObject("fd_attested_at", LocalDateTime.class);
        return d;
    }

    /** Reads the ud_* columns of the current row; null when the row has no use detail. */
    public UseDetail readUse(ResultSet rs) throws SQLException {
        int id = rs.getInt("ud_id");
        if (rs.wasNull()) return null;
        UseDetail d = new UseDetail();
        d.id = id;
        d.useDescription = rs.getString("ud_description");
        d.eventDate = rs.getObject("ud_event_date", LocalDate.class);
        d.beneficiaries = rs.getString("ud_beneficiaries");
        d.headcount = rs.getInt("ud_headcount");
        d.place = rs.getString("ud_place");
        d.dueAt = rs.getObject("ud_due_at", LocalDateTime.class);
        d.message = rs.getString("ud_message");
        Set<FoodDetail.Category> categories = EnumSet.noneOf(FoodDetail.Category.class);
        String stored = rs.getString("ud_categories");
        if (stored != null && !stored.isEmpty()) {
            for (String value : stored.split(",")) categories.add(FoodDetail.Category.valueOf(value));
        }
        d.acceptedCategories = categories;
        d.attestedAt = rs.getObject("ud_attested_at", LocalDateTime.class);
        return d;
    }

    private static int generatedId(PreparedStatement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            keys.next();
            return keys.getInt(1);
        }
    }
}
