/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.mcp.oauth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthTokensTest {
    private static final String VERIFIER = "dBjftJeZ4CVP-mJ92K9yY6qZ9sEb7rfo4M5fdRFOrqA";
    private static final String CHALLENGE = "y369XJCBGNH-E16nEZoxIbr002YWs9OIKuuf5ExCbFg";

    @Test
    void verifiesMatchingS256Challenge() {
        assertThat(OAuthTokens.verifyPkce(VERIFIER, CHALLENGE)).isTrue();
    }

    @Test
    void rejectsWrongVerifier() {
        assertThat(OAuthTokens.verifyPkce(VERIFIER.replace('d', 'e'), CHALLENGE)).isFalse();
    }

    @Test
    void rejectsMalformedVerifier() {
        assertThat(OAuthTokens.verifyPkce("too-short", CHALLENGE)).isFalse();
        assertThat(OAuthTokens.verifyPkce(VERIFIER + "!", CHALLENGE)).isFalse();
    }

    @Test
    void generatesUrlSafeUniqueTokens() {
        final String token = OAuthTokens.generate();

        assertThat(token).matches("^[A-Za-z0-9_-]{43}$");
        assertThat(OAuthTokens.generate()).isNotEqualTo(token);
    }

    @Test
    void hashesDeterministically() {
        assertThat(OAuthTokens.hash("token")).isEqualTo(OAuthTokens.hash("token")).hasSize(64);
    }
}
