package com.aniplato.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** SQL only. The bytes of an uploaded photo or document, keyed by the path the photo and document rows hold. */
public class StoredFileDao {

    public void insert(Connection c, String filePath, byte[] content) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO stored_file (file_path, content, size_bytes, stored_at) VALUES (?, ?, ?, NOW())")) {
            ps.setString(1, filePath);
            ps.setBytes(2, content);
            ps.setInt(3, content.length);
            ps.executeUpdate();
        }
    }

    /** The bytes stored under the path, or null when there are none. */
    public byte[] findContent(Connection c, String filePath) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT content FROM stored_file WHERE file_path = ?")) {
            ps.setString(1, filePath);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBytes(1) : null;
            }
        }
    }

    public void delete(Connection c, String filePath) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM stored_file WHERE file_path = ?")) {
            ps.setString(1, filePath);
            ps.executeUpdate();
        }
    }
}
