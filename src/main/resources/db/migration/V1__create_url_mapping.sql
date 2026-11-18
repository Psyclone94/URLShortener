CREATE TABLE url_mapping (
    short_code  VARCHAR(32)   PRIMARY KEY,
    long_url    VARCHAR(2048) NOT NULL,
    owner_id    VARCHAR(64),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    expires_at  TIMESTAMPTZ,
    is_active   BOOLEAN       NOT NULL DEFAULT true,
    is_custom   BOOLEAN       NOT NULL DEFAULT false
);

CREATE INDEX idx_url_mapping_expires ON url_mapping (expires_at) WHERE expires_at IS NOT NULL;
