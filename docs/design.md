# URL Shortener: Design

Status: draft v1. Scope is a single-region system. Decisions below are the defaults; change them here before writing code that depends on them.

## 1. Requirements

### Functional
- Create a short URL from a long URL.
- Optional custom alias.
- Optional expiry time.
- Redirect short code to long URL.
- Disable or delete a link.
- Click count per link.

### Non-functional
- Redirect is the hot path: p99 under 20 ms when served from cache.
- Redirect availability target: 99.99%.
- No loss of created links once the create call returns success.
- Create latency: p99 under 200 ms is acceptable.
- Click analytics must not add latency to redirects.

### Out of scope (v1)
- Multi-region deployment.
- User accounts and authentication beyond a static API key.
- Link previews and page-title fetching.
- QR codes.

## 2. Estimates

Assumptions: 100M new URLs per month, 100:1 read-to-write ratio.

| Quantity | Average | Peak (2x) |
|---|---|---|
| Writes/s | ~40 | ~200 |
| Reads/s | ~4,000 | ~10,000 |
| Storage per record | ~500 B | |
| Records over 5 years | ~6 B | |
| Total storage | ~3 TB | |

Read-heavy workload. Cache sizing: the hot set is small because link popularity is skewed; plan for a Redis instance holding the last 7 days of active links, roughly tens of GB at most.

## 3. API contract

Base path: `/api/v1`. Redirect endpoint is at root.

### Create a short URL
`POST /api/v1/urls`

Request:
```json
{
  "longUrl": "https://example.com/some/long/path?x=1",
  "alias": "my-link",
  "expiresAt": "2027-01-01T00:00:00Z"
}
```
`alias` and `expiresAt` are optional.

Response `201 Created`:
```json
{
  "shortCode": "aZ3kQ9",
  "shortUrl": "http://localhost:8080/aZ3kQ9",
  "longUrl": "https://example.com/some/long/path?x=1",
  "expiresAt": null,
  "createdAt": "2026-11-17T10:00:00Z"
}
```

### Redirect
`GET /{shortCode}`
- `302 Found` with `Location` header to the long URL.
- `404 Not Found` if the code does not exist.
- `410 Gone` if the link is expired or disabled.

### Stats (per day)
`GET /api/v1/urls/{shortCode}/stats?from=2026-11-01&to=2026-11-30`
Response `200`:
```json
{
  "shortCode": "aZ3kQ9",
  "from": "2026-11-01",
  "to": "2026-11-30",
  "days": [
    { "date": "2026-11-17", "clicks": 42 },
    { "date": "2026-11-18", "clicks": 7 }
  ]
}
```
Days with zero clicks are omitted. `from` and `to` default to the last 30 days. Dates are UTC.

### Delete or disable
`DELETE /api/v1/urls/{shortCode}`
Response `204 No Content`. Soft delete: sets `is_active = false`.

### Error format
All API errors use RFC 7807 Problem Details (`application/problem+json`):

| Status | Meaning |
|---|---|
| 400 | Invalid URL, bad scheme, too long, malformed JSON, invalid expiry |
| 401 | Missing or invalid API key |
| 404 | Unknown short code |
| 409 | Alias already taken |
| 410 | Link expired or disabled (redirect only) |
| 429 | Rate limit exceeded |
| 500 | Unexpected error |

### Validation rules
- `longUrl` must parse as an absolute URI with scheme `http` or `https`. Reject `javascript:`, `data:`, `file:`, and others.
- Maximum `longUrl` length: 2,048 characters.
- `alias`: 3 to 32 characters, `[A-Za-z0-9_-]`. Must not collide with reserved paths such as `api`, `actuator`.
- `expiresAt` must be in the future.

## 4. Data model

```sql
CREATE TABLE url_mapping (
    short_code   VARCHAR(32)  PRIMARY KEY,
    long_url     VARCHAR(2048) NOT NULL,
    owner_id     VARCHAR(64),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at   TIMESTAMPTZ,
    is_active    BOOLEAN      NOT NULL DEFAULT true,
    is_custom    BOOLEAN      NOT NULL DEFAULT false
);

CREATE INDEX idx_url_mapping_expires ON url_mapping (expires_at) WHERE expires_at IS NOT NULL;

CREATE TABLE click_daily (
    short_code  VARCHAR(32) NOT NULL REFERENCES url_mapping (short_code),
    day         DATE        NOT NULL,
    clicks      BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (short_code, day)
);
```

Notes:
- `short_code` is the primary key, which also enforces alias uniqueness. Insert with a unique-violation check, and map the violation to 409 for custom aliases.
- Generated codes are always 7 characters. Aliases can also be 7 characters, so a clash is possible. The primary key catches it. Generated-code inserts retry with the next ID on conflict. Custom-alias inserts return 409.
- Click counting is in a separate table so analytics writes do not touch the core row.

## 5. Key decisions

### 5.1 Short code generation
Use a counter-based range allocator with base62 encoding.
- Each instance leases a block of IDs (for example 1,000) from a shared Postgres sequence. Leasing happens once per 1,000 creates, so the database is not hit per request.
- The ID is passed through a keyed bijective permutation before base62 encoding, so codes are unique but not sequential or guessable.
- Seven base62 characters cover about 3.5 trillion codes.

Rejected alternatives:
- Random codes with collision checks: an extra round trip per create, and collision rates rise with volume.
- Hashing the long URL: collisions with no clean retry, and identical URLs would map to one code even when the owner differs.

### 5.2 Storage
Postgres 16 for v1. Reasons: transactional uniqueness for aliases, simple operations, good fit for the data size. The repository sits behind an interface so DynamoDB can replace it later without touching the service layer.

### 5.3 Redirect status
302, not 301. A 301 lets browsers cache the redirect and skip the server, which breaks click counting and makes expiry and disabling unreliable.

### 5.4 Caching
Redis, cache-aside pattern.
- Read path: check cache, on miss read Postgres, then write cache with TTL (default 1 hour, or the remaining lifetime if the link expires sooner).
- Negative caching: cache "not found" for 60 seconds to limit database load from bogus codes.
- Deletes and disables invalidate the cache key.
- If Redis is down, the circuit breaker sends reads straight to Postgres. Redirects keep working at higher latency.

### 5.5 Analytics
Redirect publishes a click event to Kafka asynchronously and returns immediately. A consumer aggregates counts into `click_counter`. Events may be lost if Kafka is unavailable; in that case the producer logs and counts the failure as a metric. Exact counts are not a hard requirement for v1, but the discrepancy must be measurable.

### 5.6 Security
- API key in `X-API-Key` header for create, delete, and stats endpoints. Keys stored hashed.
- Rate limit create calls per key.
- No fetching of target URLs in v1, so SSRF is not in scope. If page-title fetching is added later, block private and link-local address ranges.

## 6. Components

```
api/          controllers, request and response DTOs, error handling
domain/       ShortCode, LongUrl value objects, UrlService (no framework imports)
idgen/        range allocator, base62 encoder, permutation
persistence/  Postgres repository adapters
cache/        Redis cache adapter
analytics/    Kafka producer and consumer
```

## 7. Metrics to expose

- `redirect.latency` histogram, tagged by cache hit or miss.
- `cache.hits` and `cache.misses` counters.
- `url.created` counter, `url.create.failures` counter.
- `click.events.published` and `click.events.failed` counters.
- `idgen.lease.count` counter.

## 8. Open questions

1. Expiry default: never expire unless set, or a default lifetime? Current default: never.
2. Soft delete retention: how long are disabled rows kept? Current default: forever in v1.
3. Stats granularity: resolved. Per day, UTC, stored in `click_daily`.

## 9. Phase 2 entry criteria

- This document reviewed and open questions resolved.
- Postgres schema created by Flyway migration `V1__create_url_mapping.sql`.