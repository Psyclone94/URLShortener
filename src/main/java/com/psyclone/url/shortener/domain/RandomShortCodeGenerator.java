package com.psyclone.url.shortener.domain;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Interim generator for phase 2. Random 7-character base62 codes.
 * Phase 3 replaces this with the range allocator and permutation.
 */
@Component
public class RandomShortCodeGenerator implements ShortCodeGenerator {

    private static final String ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int LENGTH = 7;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String next() {
        return random.ints(LENGTH, 0, ALPHABET.length())
                .mapToObj(i -> String.valueOf(ALPHABET.charAt(i)))
                .reduce("", String::concat);
    }
}
