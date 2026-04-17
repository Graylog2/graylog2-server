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

import org.assertj.core.api.Assertions;
import org.graylog.storage.opensearch3.testing.OpenSearchInstance;
import org.graylog.storage.opensearch3.testing.OpenSearchInstanceBuilder;
import org.graylog.storage.opensearch3.testing.OpenSearchTestServerExtension;
import org.graylog.testing.elasticsearch.BulkIndexRequest;
import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.InvalidWriteTargetException;
import org.graylog2.indexer.MapperParsingException;
import org.graylog2.indexer.exceptions.ResultWindowLimitExceededException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opensearch.client.opensearch.core.search.TrackHits;

import java.util.Map;

/**
 * Integration tests that verify {@link OfficialOpensearchClient#mapException} correctly
 * maps real OpenSearch error responses to the expected Graylog exception types.
 *
 * <p>Each test triggers a genuine error through the OS3 Java client against a real
 * OpenSearch instance so the exception structure (type, reason, metadata) is the one
 * the production code will actually encounter.
 */
@ExtendWith(OpenSearchTestServerExtension.class)
class OfficialOpensearchClientExceptionMappingIT {

    private static final String TEST_INDEX = "exception_mapping_test";

    private OfficialOpensearchClient client;
    private SearchServerInstance searchServer;

    @BeforeEach
    void setUp(OpenSearchInstance openSearchInstance) {
        searchServer = openSearchInstance;
        client = openSearchInstance.getOfficialOpensearchClient();
    }

    @AfterEach
    void tearDown() {
        searchServer.cleanUp();
    }

    @Test
    void searchingNonExistentIndexThrowsIndexNotFoundException() {
        Assertions.assertThatThrownBy(() ->
                        client.sync(c -> c.search(r -> r.index("does_not_exist"), Map.class), "test"))
                .isInstanceOf(IndexNotFoundException.class);
    }

    @Test
    void searchingClosedIndexThrowsIndexNotFoundException() {
        searchServer.client().createIndex(TEST_INDEX);
        searchServer.client().closeIndex(TEST_INDEX);

        Assertions.assertThatThrownBy(() ->
                        client.sync(c -> c.search(r -> r.index(TEST_INDEX), Map.class), "test"))
                .isInstanceOf(IndexNotFoundException.class);
    }

    @Test
    void indexingWithFieldTypeMismatchThrowsMapperParsingException() {
        // Create index with an explicit integer mapping for "count"
        client.sync(c -> c.indices().create(r -> r
                .index(TEST_INDEX)
                .mappings(m -> m.properties("count", p -> p.integer(i -> i)))
                .settings(s -> s.numberOfShards(1).numberOfReplicas(0))), "setup");

        // Index a document where "count" is not parseable as integer
        Assertions.assertThatThrownBy(() ->
                        client.sync(c -> c.index(r -> r
                                .index(TEST_INDEX)
                                .document(Map.of("count", "not_a_number"))), "test"))
                .isInstanceOf(MapperParsingException.class);
    }

    @Test
    void indexingIntoAliasWithoutWriteIndexThrowsInvalidWriteTargetException() {
        // Create an index and add a non-write alias
        searchServer.client().createIndex(TEST_INDEX);
        searchServer.client().addAliasMapping(TEST_INDEX, "test_alias");

        // Create a second index behind the same alias so there is ambiguity
        final String secondIndex = TEST_INDEX + "_2";
        searchServer.client().createIndex(secondIndex);
        searchServer.client().addAliasMapping(secondIndex, "test_alias");

        Assertions.assertThatThrownBy(() ->
                        client.sync(c -> c.index(r -> r
                                .index("test_alias")
                                .document(Map.of("msg", "hello"))), "test"))
                .isInstanceOf(InvalidWriteTargetException.class);
    }

    @Test
    void searchBeyondMaxResultWindowThrowsResultWindowLimitExceededException() {
        // Create index with a very small result window
        client.sync(c -> c.indices().create(r -> r
                .index(TEST_INDEX)
                .settings(s -> s.numberOfShards(1).numberOfReplicas(0).maxResultWindow(1))), "setup");

        // Index a document so the index is non-empty
        final BulkIndexRequest bulk = new BulkIndexRequest();
        bulk.addRequest(TEST_INDEX, Map.of("msg", "hello"));
        bulk.addRequest(TEST_INDEX, Map.of("msg", "world"));
        searchServer.client().bulkIndex(bulk);
        searchServer.client().refreshNode();

        // Search with from beyond the window limit
        Assertions.assertThatThrownBy(() ->
                        client.sync(c -> c.search(r -> r
                                .index(TEST_INDEX)
                                .from(2)
                                .size(1)
                                .trackTotalHits(TrackHits.builder().enabled(true).build()), Map.class), "test"))
                .isInstanceOf(ResultWindowLimitExceededException.class);
    }
}
