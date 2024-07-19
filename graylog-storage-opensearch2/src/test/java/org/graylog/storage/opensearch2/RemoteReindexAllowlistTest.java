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
package org.graylog.storage.opensearch2;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;

class RemoteReindexAllowlistTest {

    @Test
    void testMultipleAllowlistValues() {
        final RemoteReindexAllowlist allowlist = new RemoteReindexAllowlist(URI.create("http://10.0.1.28:9200"), "10.0.1.28:9200, 10.0.1.49:9200,10.0.1.134:9200,10.0.1.148:9200");
        Assertions.assertThat(allowlist.value())
                .hasSize(4)
                .contains("10.0.1.28:9200", "10.0.1.49:9200", "10.0.1.134:9200", "10.0.1.148:9200");

        Assertions.assertThat(allowlist.isClusterSettingMatching("")).isFalse();
        Assertions.assertThat(allowlist.isClusterSettingMatching("10.0.1.28:9200, 10.0.1.49:9200, 10.0.1.134:9200, 10.0.1.148:9200")).isTrue();
    }

    @Test
    void testRegexValidationThrowing() {
        final RemoteReindexAllowlist allowlist = new RemoteReindexAllowlist(URI.create("http://10.0.1.28:9200"), "10.0.2.*:9200");
        Assertions.assertThatThrownBy(allowlist::validate)
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testRegexValidationOK() {
        final RemoteReindexAllowlist allowlist = new RemoteReindexAllowlist(URI.create("http://10.0.1.28:9200"), "10.0.1.*:9200");
        allowlist.validate();
    }

    @Test
    void valueWithHttpProtocol() {
        final RemoteReindexAllowlist allowlist = new RemoteReindexAllowlist(URI.create("http://example.com:9200"), "http://example.com:9200");
        Assertions.assertThat(allowlist.value())
                .hasSize(1)
                .contains("example.com:9200");
    }


    @Test
    void valueWithHttpsProtocol() {
        final RemoteReindexAllowlist allowlist = new RemoteReindexAllowlist(URI.create("https://example.com:9200"), "https://example.com:9200");
        Assertions.assertThat(allowlist.value())
                .containsExactly("example.com:9200");
    }

    @Test
    void valueWithoutProtocol() {
        final RemoteReindexAllowlist allowlist = new RemoteReindexAllowlist(URI.create("http://example.com:9200"), "example.com:9200");
        Assertions.assertThat(allowlist.value())
                .containsExactly("example.com:9200");
    }
}
