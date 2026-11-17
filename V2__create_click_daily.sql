CREATE TABLE click_daily (
    short_code  VARCHAR(32) NOT NULL REFERENCES url_mapping (short_code),
    day         DATE        NOT NULL,
    clicks      BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (short_code, day)
);