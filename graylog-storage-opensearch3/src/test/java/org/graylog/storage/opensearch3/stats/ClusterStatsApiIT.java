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

import java.util.Map;

class ClusterStatsApiIT {

    @Rule
    public final OpenSearchInstance opensearch = OpenSearchInstance.create();
    private ClusterStatsApi api;

    @BeforeEach
    void setUp() {
        api = new ClusterStatsApi(opensearch.getOfficialOpensearchClient());
    }

    @Test
    void testStats() {
        opensearch.withTestableEnvironment((environment) -> {
            // capture numbers for existing cluster, whatever that means
            final IndexSetStats before = api.clusterStats();

            environment.createIndex().indexDocuments(10, i -> Map.of("foo", i));

            // capture numbers again, they should be already refreshed by the previous generateIndex call
            final IndexSetStats after = api.clusterStats();

            // verify that *after* values are the *before* plus 1 index with 10 docs
            Assertions.assertThat(after.indices()).isGreaterThanOrEqualTo(before.indices() + 1);
            Assertions.assertThat(after.documents()).isGreaterThanOrEqualTo(before.documents() + 10);
            Assertions.assertThat(after.size()).isGreaterThan(before.size());
        });
    }


}
