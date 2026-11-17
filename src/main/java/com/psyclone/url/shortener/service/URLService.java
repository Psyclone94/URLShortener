package com.psyclone.url.shortener.service;

import com.psyclone.url.shortener.repository.UrlRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import com.psyclone.url.shortener.domain.ShortCodeGenerator;
import com.psyclone.url.shortener.domain.UrlMapping;
import com.psyclone.url.shortener.domain.UrlErrors;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class URLService {

    private static final int MAX_URL_LENGTH = 2048;
    private static final int MAX_GENERATION_ATTEMPTS = 5;
    private static final Pattern ALIAS_PATTERN = Pattern.compile("[A-Za-z0-9_-]{3,32}");
    private static final Set<String> RESERVED_ALIASES = Set.of("api", "actuator", "error");
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private final UrlRepository repository;
    private final ShortCodeGenerator codeGenerator;

    public URLService(UrlRepository repository, ShortCodeGenerator codeGenerator) {
        this.repository = repository;
        this.codeGenerator = codeGenerator;
    }

    public UrlMapping create(String longUrl, String alias, OffsetDateTime expiresAt) {
        validateLongUrl(longUrl);
        validateExpiry(expiresAt);

        if (alias != null) {
            return createWithAlias(longUrl, alias, expiresAt);
        }
        return createWithGeneratedCode(longUrl, expiresAt);
    }

    public UrlMapping resolve(String shortCode) {
        UrlMapping mapping = repository.findByCode(shortCode)
                .orElseThrow(() -> new UrlErrors.NotFound(shortCode));

        boolean expired = mapping.expiresAt() != null
                && !mapping.expiresAt().isAfter(now());

        if (!mapping.isActive() || expired) {
            throw new UrlErrors.Gone(shortCode);
        }
        return mapping;
    }

    private UrlMapping createWithAlias(String longUrl, String alias, OffsetDateTime expiresAt) {
        if (!ALIAS_PATTERN.matcher(alias).matches()) {
            throw new UrlErrors.InvalidUrl("Alias must be 3 to 32 characters: A-Z, a-z, 0-9, _ or -");
        }
        if (RESERVED_ALIASES.contains(alias.toLowerCase(Locale.ROOT))) {
            throw new UrlErrors.InvalidUrl("Alias is reserved: " + alias);
        }
        var mapping = new UrlMapping(alias, longUrl, now(), expiresAt, true, true);
        try {
            repository.insert(mapping);
            return mapping;
        } catch (DuplicateKeyException e) {
            throw new UrlErrors.AliasTaken(alias);
        }
    }

    private UrlMapping createWithGeneratedCode(String longUrl, OffsetDateTime expiresAt) {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            var mapping = new UrlMapping(codeGenerator.next(), longUrl, now(), expiresAt, true, false);
            try {
                repository.insert(mapping);
                return mapping;
            } catch (DuplicateKeyException ignored) {
                // Code collided with an existing row; try again.
            }
        }
        throw new IllegalStateException("Could not generate a unique short code");
    }

    private static void validateLongUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new UrlErrors.InvalidUrl("longUrl is required");
        }
        if (raw.length() > MAX_URL_LENGTH) {
            throw new UrlErrors.InvalidUrl("longUrl exceeds " + MAX_URL_LENGTH + " characters");
        }
        URI uri;
        try {
            uri = new URI(raw);
        } catch (URISyntaxException e) {
            throw new UrlErrors.InvalidUrl("longUrl is not a valid URI");
        }
        if (!uri.isAbsolute() || uri.getHost() == null
                || !ALLOWED_SCHEMES.contains(uri.getScheme().toLowerCase(Locale.ROOT))) {
            throw new UrlErrors.InvalidUrl("longUrl must be an absolute http or https URL");
        }
    }

    private static void validateExpiry(OffsetDateTime expiresAt) {
        if (expiresAt != null && !expiresAt.isAfter(now())) {
            throw new UrlErrors.InvalidUrl("expiresAt must be in the future");
        }
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
