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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RedirectUriPolicyTest {
    private static final List<String> ALLOWED = List.of("https://claude.ai/api/mcp/auth_callback");

    @ParameterizedTest
    @ValueSource(strings = {
            "http://127.0.0.1:33418/callback",
            "http://localhost:6274/oauth/callback",
            "http://[::1]:8080/cb",
            "https://claude.ai/api/mcp/auth_callback",
            "cursor://anysphere.cursor-retrieval/oauth/user-graylog/callback"
    })
    void allowsLoopbackAllowlistedAndNativeAppUris(String uri) {
        assertThat(RedirectUriPolicy.isAllowed(uri, ALLOWED)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://evil.example.com/callback",
            "http://evil.example.com/callback",
            "https://claude.ai/api/mcp/auth_callback/other",
            "https://127.0.0.1/callback",
            "http://127.0.0.1:8080/cb#fragment",
            "http://user@127.0.0.1:8080/cb",
            "javascript:alert(1)",
            "data:text/html,hi",
            "/relative/callback",
            ""
    })
    void rejectsOtherUris(String uri) {
        assertThat(RedirectUriPolicy.isAllowed(uri, ALLOWED)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "http://127.0.0.1:1234/callback, http://127.0.0.1:5678/callback, http://127.0.0.1:5678/callback",
            "http://127.0.0.1/callback, http://127.0.0.1:5678/callback, http://127.0.0.1:5678/callback",
            "http://127.0.0.1:1234/callback, http://127.0.0.1/callback, http://127.0.0.1/callback",
            "http://[::1]:1234/cb?a=%20b, http://[::1]:5678/cb?a=%20b, http://[::1]:5678/cb?a=%20b",
            "http://localhost:1234/callback, http://LOCALHOST:5678/callback, http://localhost:5678/callback",
            "https://claude.ai/api/mcp/auth_callback, https://claude.ai/api/mcp/auth_callback, https://claude.ai/api/mcp/auth_callback"
    })
    void resolvesRegisteredUris(String registered, String requested, String expected) {
        assertThat(RedirectUriPolicy.resolve(registered, requested)).contains(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "http://127.0.0.1:1234/callback, http://127.0.0.1:1234/other",
            "http://127.0.0.1:1234/callback, http://localhost:1234/callback",
            "http://127.0.0.1:1234/callback, http://127.0.0.1:1234/callback?x=1",
            "https://claude.ai:443/cb, https://claude.ai:8443/cb"
    })
    void rejectsUnregisteredUris(String registered, String requested) {
        assertThat(RedirectUriPolicy.resolve(registered, requested)).isEmpty();
    }
}
