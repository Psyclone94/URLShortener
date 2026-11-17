package com.psyclone.url.shortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public final class UrlDtos {

    private UrlDtos() {
    }

    public record CreateUrlRequest(
            @NotBlank @Size(max = 2048) String longUrl,
            @Pattern(regexp = "[A-Za-z0-9_-]{3,32}", message = "alias must be 3 to 32 characters") String alias,
            OffsetDateTime expiresAt
    ) {
    }

    public record CreateUrlResponse(
            String shortCode,
            String shortUrl,
            String longUrl,
            OffsetDateTime expiresAt,
            OffsetDateTime createdAt
    ) {
    }
}
