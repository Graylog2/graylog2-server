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
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.management.CatIndex;
import org.graylog2.indexer.management.CatShard;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IndexManagementAdapterOSIT {
    private static final String INDEX = "index-management-it";

    @SearchInstance
    public final OpenSearchInstance openSearchInstance = OpenSearchInstance.create();

    private IndexManagementAdapterOS adapter;

    @BeforeEach
    void setUp() {
        adapter = new IndexManagementAdapterOS(openSearchInstance.getOfficialOpensearchClient(), new ObjectMapperProvider().get());
        // One node: the replica can't be placed, which gives an unassigned copy to explain. Creating the index waits
        // for its primary to start.
        openSearchInstance.client().createIndex(INDEX, 1, 1);
    }

    @AfterEach
    void tearDown() {
        openSearchInstance.client().deleteIndices(INDEX);
    }

    @Test
    void listsTheIndexWithItsColumns() {
        assertThat(adapter.indices()).filteredOn(row -> INDEX.equals(row.index())).singleElement().satisfies(row -> {
            assertThat(row.status()).isEqualTo("open");
            assertThat(row.health()).isEqualTo("yellow");
            assertThat(row.primaryShards()).isEqualTo(1);
            assertThat(row.replicas()).isEqualTo(1);
            assertThat(row.storeSizeBytes()).isNotNull();
        });
        assertThat(adapter.indices()).extracting(CatIndex::index).doesNotContainNull();
    }

    @Test
    void listsShardCopiesWithWhyTheReplicaIsUnassigned() {
        assertThat(adapter.shards()).filteredOn(copy -> INDEX.equals(copy.index())).satisfiesExactlyInAnyOrder(
                primary -> {
                    assertThat(primary.primary()).isTrue();
                    assertThat(primary.state()).isEqualTo("STARTED");
                    assertThat(primary.node()).isNotBlank();
                },
                replica -> {
                    assertThat(replica.primary()).isFalse();
                    assertThat(replica.isUnassigned()).isTrue();
                    assertThat(replica.unassignedReason()).isEqualTo("INDEX_CREATED");
                    assertThat(replica.unassignedAt()).isNotBlank();
                });
        assertThat(adapter.shards()).extracting(CatShard::index).doesNotContainNull();
    }

    @Test
    void listsTheNodes() {
        assertThat(adapter.nodes()).singleElement().satisfies(node -> {
            assertThat(node.id()).isNotBlank();
            assertThat(node.name()).isNotBlank();
        });
    }

    @Test
    void explainsTheUnassignedReplica() {
        assertThat(adapter.allocationExplain(INDEX, 0, false).path("can_allocate").asText()).isEqualTo("no");
    }

    @Test
    void failsInsteadOfAnsweringEmptyForAMissingIndex() {
        assertThatThrownBy(() -> adapter.allocationExplain("no-such-index", 0, true)).isInstanceOf(IndexNotFoundException.class);
        assertThatThrownBy(() -> adapter.clearCache("no-such-index")).isInstanceOf(IndexNotFoundException.class);
    }

    @Test
    void clearsTheCacheRetriesAndFindsNoWarmIndices() {
        adapter.clearCache(INDEX);

        assertThat(adapter.retryFailedAllocations()).isTrue();
        assertThat(adapter.warmIndices()).doesNotContain(INDEX);
    }
}
