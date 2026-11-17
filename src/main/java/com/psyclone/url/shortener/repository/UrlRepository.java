package com.psyclone.url.shortener.repository;

import com.psyclone.url.shortener.domain.UrlMapping;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.Optional;

@Repository
public class UrlRepository {

    private final JdbcClient jdbc;

    public UrlRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Throws DuplicateKeyException if the short code already exists. */
    public void insert(UrlMapping m) {
        jdbc.sql("""
                INSERT INTO url_mapping (short_code, long_url, created_at, expires_at, is_active, is_custom)
                VALUES (:shortCode, :longUrl, :createdAt, :expiresAt, :isActive, :isCustom)
                """)
                .param("shortCode", m.shortCode())
                .param("longUrl", m.longUrl())
                .param("createdAt", m.createdAt(), Types.TIMESTAMP_WITH_TIMEZONE)
                .param("expiresAt", m.expiresAt(), Types.TIMESTAMP_WITH_TIMEZONE)
                .param("isActive", m.isActive())
                .param("isCustom", m.isCustom())
                .update();
    }

    public Optional<UrlMapping> findByCode(String shortCode) {
        return jdbc.sql("""
                SELECT short_code, long_url, created_at, expires_at, is_active, is_custom
                FROM url_mapping
                WHERE short_code = :shortCode
                """)
                .param("shortCode", shortCode)
                .query(UrlMapping.class)
                .optional();
    }
}
