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
package org.graylog.storage.opensearch3;

import org.graylog.storage.opensearch3.testing.OpenSearchInstance;
import org.graylog.testing.elasticsearch.SearchInstance;
import org.graylog.testing.storage.SearchServer;
import org.graylog2.storage.SearchVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NodeAdapterOSIT {

    @SearchInstance
    public final OpenSearchInstance openSearchInstance = OpenSearchInstance.create();

    private NodeAdapterOS adapter;

    @BeforeEach
    void setUp() {
        adapter = new NodeAdapterOS(openSearchInstance.getOfficialOpensearchClient());
    }

    @Test
    void testOpensearchVersionFetching() {
        assertThat(adapter.version())
                .isNotEmpty()
                .contains(SearchVersion.create(SearchVersion.Distribution.OPENSEARCH, SearchServer.DEFAULT_VERSION.version()));
    }
}
