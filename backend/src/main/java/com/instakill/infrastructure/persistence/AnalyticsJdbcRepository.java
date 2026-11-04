package com.instakill.infrastructure.persistence;

import com.instakill.analytics.application.AnalyticsRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * All statistics are computed in SQL (SQLite). Timestamps are stored as epoch milliseconds,
 * UUIDs as CHAR(36) strings.
 */
@Repository
public class AnalyticsJdbcRepository implements AnalyticsRepository {

    private static final String VIDEO_PREDICATE =
            "(lower(p.image_url) LIKE '%.mp4' OR lower(p.image_url) LIKE '%.mov')";

    /** Per-post like and comment counts, computed from the actual rows (not the denormalized counters). */
    private static final String POST_ENGAGEMENT = """
            SELECT p.id, p.author_id, p.content, p.image_url, p.created_at,
                   COALESCE(l.c, 0) AS likes, COALESCE(c.c, 0) AS comments
            FROM posts p
            LEFT JOIN (SELECT post_id, COUNT(*) AS c FROM likes GROUP BY post_id) l ON l.post_id = p.id
            LEFT JOIN (SELECT post_id, COUNT(*) AS c FROM comments GROUP BY post_id) c ON c.post_id = p.id
            """;

    private final JdbcTemplate jdbc;

    public AnalyticsJdbcRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<WeeklyStats> weeklyStats(UUID userId) {
        // Week = Monday of the week in which the post/like/comment was created (UTC).
        String week = "date(%s / 1000, 'unixepoch', 'weekday 0', '-6 days')";
        String sql = """
                SELECT week, SUM(posts) AS posts, SUM(likes) AS likes, SUM(comments) AS comments FROM (
                    SELECT %1$s AS week, 1 AS posts, 0 AS likes, 0 AS comments
                    FROM posts p WHERE p.author_id = ?
                  UNION ALL
                    SELECT %2$s, 0, 1, 0
                    FROM likes x JOIN posts p ON p.id = x.post_id WHERE p.author_id = ?
                  UNION ALL
                    SELECT %3$s, 0, 0, 1
                    FROM comments x JOIN posts p ON p.id = x.post_id WHERE p.author_id = ?
                )
                GROUP BY week ORDER BY week
                """.formatted(week.formatted("p.created_at"), week.formatted("x.created_at"),
                week.formatted("x.created_at"));
        String id = userId.toString();
        return jdbc.query(sql,
                (rs, i) -> new WeeklyStats(rs.getString("week"), rs.getLong("posts"), rs.getLong("likes"),
                        rs.getLong("comments")),
                id, id, id);
    }

    @Override
    public List<TopPost> topPostsByEngagement(UUID userId, int limit) {
        String sql = "SELECT id, content, image_url, likes, comments FROM (" + POST_ENGAGEMENT + ") "
                + "WHERE author_id = ? ORDER BY likes + comments DESC, created_at DESC LIMIT ?";
        return jdbc.query(sql,
                (rs, i) -> new TopPost(UUID.fromString(rs.getString("id")), rs.getString("content"),
                        rs.getString("image_url"), rs.getLong("likes"), rs.getLong("comments")),
                userId.toString(), limit);
    }

    @Override
    public Totals totals(UUID userId) {
        String sql = "SELECT COUNT(*) AS posts, COALESCE(SUM(likes), 0) AS likes, COALESCE(SUM(comments), 0) AS comments "
                + "FROM (" + POST_ENGAGEMENT + ") WHERE author_id = ?";
        return jdbc.queryForObject(sql,
                (rs, i) -> new Totals(rs.getLong("posts"), rs.getLong("likes"), rs.getLong("comments")),
                userId.toString());
    }

    @Override
    public List<MediaTypeAverage> averageLikesByMediaType(UUID userId) {
        String sql = """
                SELECT CASE WHEN %s THEN 'video' ELSE 'photo' END AS media_type,
                       COUNT(*) AS posts, AVG(likes) AS avg_likes
                FROM (%s) p
                WHERE p.author_id = ? AND p.image_url IS NOT NULL AND p.image_url <> ''
                GROUP BY media_type
                """.formatted(VIDEO_PREDICATE, POST_ENGAGEMENT);
        return jdbc.query(sql,
                (rs, i) -> new MediaTypeAverage(rs.getString("media_type"), rs.getLong("posts"),
                        rs.getDouble("avg_likes")),
                userId.toString());
    }

    @Override
    public Optional<UserRank> engagementRank(UUID userId) {
        String sql = """
                WITH per_user AS (
                    SELECT author_id, AVG(likes + comments) AS avg_eng FROM (%s) GROUP BY author_id
                )
                SELECT me.avg_eng AS avg_eng,
                       (SELECT COUNT(*) FROM per_user) AS total_users,
                       (SELECT COUNT(*) FROM per_user o WHERE o.avg_eng > me.avg_eng) + 1 AS rank
                FROM per_user me WHERE me.author_id = ?
                """.formatted(POST_ENGAGEMENT);
        return jdbc.query(sql,
                (rs, i) -> new UserRank(rs.getDouble("avg_eng"), rs.getLong("rank"), rs.getLong("total_users")),
                userId.toString()).stream().findFirst();
    }
}
