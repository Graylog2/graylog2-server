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

import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthClientIdsTest {
    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");
    private static final List<String> REDIRECT_URIS = List.of("http://127.0.0.1:33418/callback");

    private final OAuthClientIds clientIds = new OAuthClientIds("password-secret", new ObjectMapperProvider().get());

    @Test
    void decodesIssuedClient() {
        final OAuthClient client = clientIds.issue("Claude Code", "https://claude.ai", REDIRECT_URIS, NOW).orElseThrow();

        assertThat(client.clientId()).startsWith("mcp1.");
        assertThat(clientIds.decode(client.clientId())).contains(
                new OAuthClient(client.clientId(), "Claude Code", "https://claude.ai", REDIRECT_URIS, NOW));
    }

    @Test
    void issuesDistinctClientIdsForSameMetadata() {
        final OAuthClient first = clientIds.issue("Claude Code", null, REDIRECT_URIS, NOW).orElseThrow();
        final OAuthClient second = clientIds.issue("Claude Code", null, REDIRECT_URIS, NOW).orElseThrow();

        assertThat(first.clientId()).isNotEqualTo(second.clientId());
    }

    @Test
    void rejectsTamperedPayload() {
        final String clientId = clientIds.issue("Claude Code", null, REDIRECT_URIS, NOW).orElseThrow().clientId();
        final String[] parts = clientId.split("\\.");
        final OAuthClient forged = new OAuthClientIds("other-secret", new ObjectMapperProvider().get())
                .issue("Claude Code", null, List.of("https://evil.example.com/cb"), NOW).orElseThrow();
        final String forgedPayload = forged.clientId().split("\\.")[1];

        assertThat(clientIds.decode(forged.clientId())).isEmpty();
        assertThat(clientIds.decode(String.join(".", parts[0], forgedPayload, parts[2]))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "mcp-0b1f6a8e-6a3c-4a51-9d0e-2a6f1c1b2f3e", "mcp1.", "mcp1.abc", "mcp1.abc.def", "mcp1.a.b.c", "mcp1.!!!.???"})
    void rejectsMalformedClientIds(String clientId) {
        assertThat(clientIds.decode(clientId)).isEmpty();
    }

    @Test
    void refusesClientIdsAboveMaximumLength() {
        final List<String> redirectUris = List.of(
                "http://127.0.0.1/" + "a".repeat(1500),
                "http://127.0.0.1/" + "b".repeat(1500));

        assertThat(clientIds.issue("Claude Code", null, redirectUris, NOW)).isEmpty();
        assertThat(clientIds.decode("mcp1." + "a".repeat(OAuthClientIds.MAX_LENGTH))).isEmpty();
    }
}
