package com.urlshortener.repository;

import com.urlshortener.exception.DuplicateAliasException;
import com.urlshortener.model.UrlListItem;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class ShortenedUrlRepository {

    private final JdbcTemplate jdbc;

    public ShortenedUrlRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void init() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS shortened_urls ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "alias TEXT NOT NULL UNIQUE, "
                + "full_url TEXT NOT NULL, "
                + "created_at TEXT NOT NULL"
                + ")");
    }

    public void save(String alias, String fullUrl, Instant createdAt) {
        // skip alias conflicts but show other database failures
        int rows = jdbc.update("""
                        INSERT INTO shortened_urls (alias, full_url, created_at) VALUES (?, ?, ?)
                        ON CONFLICT(alias) DO NOTHING
                        """,
                alias, fullUrl, createdAt.toString());
        if (rows == 0) {
            throw new DuplicateAliasException(alias);
        }
    }

    public Optional<String> findFullUrlByAlias(String alias) {
        var results = jdbc.query("SELECT full_url FROM shortened_urls WHERE alias = ? LIMIT 1",
                (rs, rowNum) -> rs.getString("full_url"),
                alias);
        return results.stream().findFirst();
    }

    public List<UrlListItem> findAll(String baseUrl, int size, long offset) {
        return jdbc.query(
                """
                SELECT alias, full_url
                FROM shortened_urls
                ORDER BY created_at DESC, id DESC
                LIMIT ? OFFSET ?
                """,
                urlListItemMapper(baseUrl),
                size,
                offset
        );
    }

    public long count() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM shortened_urls", Long.class);
    }

    public boolean deleteByAlias(String alias) {
        var rows = jdbc.update("DELETE FROM shortened_urls WHERE alias = ?", alias);
        return rows > 0;
    }

    private RowMapper<UrlListItem> urlListItemMapper(String baseUrl) {
        return (rs, rowNum) -> new UrlListItem(
                rs.getString("alias"),
                rs.getString("full_url"),
                baseUrl + "/" + rs.getString("alias")
        );
    }


}
