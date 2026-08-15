package com.base.project.spring.boot.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenHasherTest {

    private final TokenHasher hasher = new TokenHasher();

    @Test
    void hash_isDeterministic() {
        assertThat(hasher.hash("some-token")).isEqualTo(hasher.hash("some-token"));
    }

    @Test
    void hash_differsForDifferentInput() {
        assertThat(hasher.hash("token-a")).isNotEqualTo(hasher.hash("token-b"));
    }

    @Test
    void hash_neverEqualsRawInput() {
        assertThat(hasher.hash("some-token")).isNotEqualTo("some-token");
    }

    @Test
    void hash_isBase64EncodedSha256() {
        // SHA-256 digest is 32 bytes -> 44 base64 characters (including padding).
        assertThat(hasher.hash("some-token")).hasSize(44);
    }

}
