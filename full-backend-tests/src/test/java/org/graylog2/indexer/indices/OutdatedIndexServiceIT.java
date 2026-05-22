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

package org.graylog2.indexer.indices;

import org.graylog.testing.completebackend.FullBackendTest;
import org.graylog.testing.completebackend.GraylogBackendConfiguration;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.elasticsearch.BulkIndexRequest;
import org.graylog.testing.elasticsearch.SearchServerBaseTest;
import org.graylog2.indexer.counts.CountsAdapter;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@GraylogBackendConfiguration(serverLifecycle = Lifecycle.CLASS)
public class OutdatedIndexServiceIT extends SearchServerBaseTest {

    OutdatedIndexService outdatedIndexService;
    IndicesAdapter indicesAdapter;
    CountsAdapter countsAdapter;

    @BeforeEach
    public void setUp() throws Exception {
        indicesAdapter = searchServer().adapters().indicesAdapter();
        countsAdapter = searchServer().adapters().countsAdapter();
        outdatedIndexService = new OutdatedIndexService(indicesAdapter, null, null);
    }

    @FullBackendTest
    public void testReindexingSuccessful() {
        String toReindex = client().createRandomIndex("reindextest");
        BulkIndexRequest bulkIndexRequest = new BulkIndexRequest();
        long messageCount = 10L;
        for (int i = 0; i < messageCount; i++) {
            bulkIndexRequest.addRequest(toReindex, Map.of("foo", "bar" + i));
        }
        client().bulkIndex(bulkIndexRequest);
        String originalId = indicesAdapter.getIndexId(toReindex);
        Optional<DateTime> originalCreationDate = indicesAdapter.indexCreationDate(toReindex);
        assertThat(countsAdapter.totalCount(List.of(toReindex))).isEqualTo(messageCount);
        outdatedIndexService.reindex(toReindex, true);
        assertThat(countsAdapter.totalCount(List.of(toReindex))).isEqualTo(messageCount);
        assertThat(indicesAdapter.getIndexId(toReindex)).isNotEqualTo(originalId);
        assertThat(indicesAdapter.indexCreationDate(toReindex).get()).isNotEqualTo(originalCreationDate.get());
        indicesAdapter.delete(toReindex);
    }


}
