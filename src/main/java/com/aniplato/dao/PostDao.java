package com.aniplato.dao;

import com.aniplato.model.Post;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** SQL only. One DAO per table. Listings and requests (one table, told apart by kind), plus the post totals for dashboards and reports. */
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

    //---LISTINGS AND REQUESTS---

    private static final String POST_SELECT = """
            SELECT p.post_id, p.kind, p.posted_by, p.kg, p.status, p.cancel_reason, p.created_at,
                   city.name AS city_name, r.name AS poster_name,
            """ + DetailDao.FOOD_COLUMNS + ",\n" + DetailDao.USE_COLUMNS + """

            FROM post p
            JOIN city ON city.city_id = p.city_id
            JOIN account a ON a.account_id = p.posted_by
            JOIN registration r ON r.registration_id = a.registration_id
            LEFT JOIN food_detail fd ON fd.food_detail_id = p.food_detail_id
            LEFT JOIN use_detail ud ON ud.use_detail_id = p.use_detail_id
            """;

    private final DetailDao detailDao = new DetailDao();

    /** Saves the post row for a detail already inserted. Exactly one of the two detail ids is set (the CHECK needs it). */
    public int insert(Connection c, Post.Kind kind, int postedBy, int cityId, double kg,
                      Integer foodDetailId, Integer useDetailId, LocalDateTime createdAt) throws SQLException {
        String sql = "INSERT INTO post (kind, posted_by, city_id, kg, food_detail_id, use_detail_id, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, kind.name());
            ps.setInt(2, postedBy);
            ps.setInt(3, cityId);
            ps.setDouble(4, kg);
            ps.setObject(5, foodDetailId, Types.INTEGER);
            ps.setObject(6, useDetailId, Types.INTEGER);
            ps.setObject(7, createdAt);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public Post findById(Connection c, int postId) throws SQLException {
        List<Post> found = query(c, POST_SELECT + "WHERE p.post_id = ?", postId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** An Open post from an Active poster, locked until the transaction ends so an accept cannot slip in between a check and a write. */
    public Post findOpenForUpdate(Connection c, int postId) throws SQLException {
        List<Post> found = query(c, POST_SELECT + "WHERE p.post_id = ? AND p.status = 'OPEN' AND a.status = 'ACTIVE' FOR UPDATE", postId);
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * Open posts of one kind from Active posters (a Suspended poster's posts stay hidden, 6.3), the viewer's city
     * first and then newest first. Offers are not counted here: this is the other side's board.
     */
    public List<Post> findOpenBoard(Connection c, Post.Kind kind, int viewerCityId) throws SQLException {
        return query(c, POST_SELECT + "WHERE p.kind = ? AND p.status = 'OPEN' AND a.status = 'ACTIVE' "
                + "ORDER BY (p.city_id = ?) DESC, p.created_at DESC, p.post_id DESC", kind.name(), viewerCityId);
    }

    /** Every post one account made, newest first, with the number of offers each has received. */
    public List<Post> findByPoster(Connection c, int posterId) throws SQLException {
        List<Post> posts = query(c, POST_SELECT + "WHERE p.posted_by = ? ORDER BY p.created_at DESC, p.post_id DESC", posterId);
        for (Post post : posts) post.offerCount = countOffers(c, post.id);
        return posts;
    }

    private int countOffers(Connection c, int postId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM offer WHERE post_id = ?")) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private List<Post> query(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            try (ResultSet rs = ps.executeQuery()) {
                List<Post> posts = new ArrayList<>();
                while (rs.next()) posts.add(read(rs));
                return posts;
            }
        }
    }

    private Post read(ResultSet rs) throws SQLException {
        Post p = new Post();
        p.id = rs.getInt("post_id");
        p.kind = Post.Kind.valueOf(rs.getString("kind"));
        p.posterId = rs.getInt("posted_by");
        p.posterName = rs.getString("poster_name");
        p.city = rs.getString("city_name");
        p.kg = rs.getDouble("kg");
        p.status = Post.Status.valueOf(rs.getString("status"));
        p.cancelReason = rs.getString("cancel_reason");
        p.createdAt = rs.getObject("created_at", LocalDateTime.class);
        p.food = detailDao.readFood(rs);
        p.use = detailDao.readUse(rs);
        return p;
    }
}
