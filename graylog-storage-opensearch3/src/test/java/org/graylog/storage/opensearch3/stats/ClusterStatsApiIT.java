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
package org.graylog.storage.opensearch3.stats;

import org.assertj.core.api.Assertions;
import org.graylog.storage.opensearch3.testing.OpenSearchInstance;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetStats;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.core.BulkRequest;

import java.util.Map;
import java.util.stream.IntStream;

class ClusterStatsApiIT {
    public static final String INDEX_NAME = "my_index_0";
    @Rule
    public final OpenSearchInstance opensearch = OpenSearchInstance.create();
    private ClusterStatsApi api;

    @BeforeEach
    void setUp() {
        api = new ClusterStatsApi(opensearch.getOfficialOpensearchClient());
    }

    @Test
    void testStats() {
        // capture numbers for existing cluster, whatever that means
        final IndexSetStats before = api.clusterStats();

        // create new index with 10 docs
        generateIndex(INDEX_NAME, 10);

        // capture numbers again, they should be already refreshed by the previous generateIndex call
        final IndexSetStats after = api.clusterStats();

        // verify that *after* values are the *before* plus 1 index with 10 docs
        Assertions.assertThat(after.indices()).isGreaterThanOrEqualTo(before.indices() + 1);
        Assertions.assertThat(after.documents()).isGreaterThanOrEqualTo(before.documents() + 10);
        Assertions.assertThat(after.size()).isGreaterThan(before.size());
    }

    private void generateIndex(String indexName, int documentsCount) {
        opensearch.getOfficialOpensearchClient().sync(c -> c.indices().create(r -> r.index(indexName)), "Failed to create index");
        BulkRequest.Builder br = new BulkRequest.Builder();
        IntStream.range(0, documentsCount).forEach(i -> br.operations(op -> op.index(idx -> idx.index(indexName).document(Map.of("foo", i)))));
        opensearch.getOfficialOpensearchClient().sync(c -> c.bulk(br.build()), "Failed to index documents");
        opensearch.getOfficialOpensearchClient().sync(c -> c.indices().refresh(), "Failed to refresh indices");
    }
}
