package com.psyclone.url.shortener.domain;

import java.time.OffsetDateTime;

public record UrlMapping(
        String shortCode,
        String longUrl,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt,
        boolean isActive,
        boolean isCustom
) {
}
