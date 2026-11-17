package com.psyclone.url.shortener.domain;

public final class UrlErrors {

    private UrlErrors() {
    }

    public static final class InvalidUrl extends RuntimeException {
        public InvalidUrl(String message) {
            super(message);
        }
    }

    public static final class AliasTaken extends RuntimeException {
        public AliasTaken(String alias) {
            super("Alias already taken: " + alias);
        }
    }

    public static final class NotFound extends RuntimeException {
        public NotFound(String shortCode) {
            super("No link for code: " + shortCode);
        }
    }

    public static final class Gone extends RuntimeException {
        public Gone(String shortCode) {
            super("Link is expired or disabled: " + shortCode);
        }
    }
}