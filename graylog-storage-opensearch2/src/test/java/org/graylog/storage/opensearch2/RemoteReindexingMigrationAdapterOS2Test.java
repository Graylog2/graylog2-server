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
import org.graylog2.indexer.datanode.RemoteReindexRequest;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collections;
import java.util.List;

class RemoteReindexingMigrationAdapterOS2Test {

    @Test
    void parseAllowlistWithHttpProtocol() {
        final RemoteReindexRequest req = new RemoteReindexRequest("http://example.com:9200", URI.create("http://example.com:9200"), "", "", Collections.emptyList(), 4, false);
        final List<String> allowList = RemoteReindexingMigrationAdapterOS2.parseAllowlist(req);
        Assertions.assertThat(allowList)
                .containsExactly("example.com:9200");
    }


    @Test
    void parseAllowlistWithHttpsProtocol() {
        final RemoteReindexRequest req = new RemoteReindexRequest("https://example.com:9200", URI.create("https://example.com:9200"), "", "", Collections.emptyList(), 4, false);
        final List<String> allowList = RemoteReindexingMigrationAdapterOS2.parseAllowlist(req);
        Assertions.assertThat(allowList)
                .containsExactly("example.com:9200");
    }

    @Test
    void parseAllowlistWithoutProtocol() {
        final RemoteReindexRequest req = new RemoteReindexRequest("example.com:9200", URI.create("http://example.com:9200"), "", "", Collections.emptyList(), 4, false);
        final List<String> allowList = RemoteReindexingMigrationAdapterOS2.parseAllowlist(req);
        Assertions.assertThat(allowList)
                .containsExactly("example.com:9200");
    }
}
