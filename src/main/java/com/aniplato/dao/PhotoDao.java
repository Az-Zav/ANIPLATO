package com.aniplato.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** SQL only. A photo belongs to exactly one of a post, an offer or a report (the table's CHECK enforces it). */
public class PhotoDao {

    /** Which kind of record owns the photo. */
    public enum Owner {
        POST("post_id"), OFFER("offer_id"), REPORT("report_id");

        private final String column;

        Owner(String column) {
            this.column = column;
        }
    }

    public void insert(Connection c, Owner owner, int ownerId, String filePath) throws SQLException {
        String sql = "INSERT INTO photo (file_path, " + owner.column + ") VALUES (?, ?)";   // column comes from the enum
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, filePath);
            ps.setInt(2, ownerId);
            ps.executeUpdate();
        }
    }

    /** The stored paths for one owner, in the order they were added. */
    public List<String> findPaths(Connection c, Owner owner, int ownerId) throws SQLException {
        String sql = "SELECT file_path FROM photo WHERE " + owner.column + " = ? ORDER BY photo_id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                List<String> paths = new ArrayList<>();
                while (rs.next()) paths.add(rs.getString(1));
                return paths;
            }
        }
    }
}
