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
package org.graylog2.indexer.counts;

import com.google.common.collect.ImmutableMap;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog.testing.elasticsearch.BulkIndexRequest;
import org.graylog.testing.elasticsearch.ContainerMatrixElasticsearchBaseTest;
import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// these tests only test the SearchServer, so there is only one MongoDB-version necessary (needed, to launch the tests)
@ContainerMatrixTestsConfiguration(mongoVersions = MongodbServer.MONGO5)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CountsIT extends ContainerMatrixElasticsearchBaseTest {
    private static final String INDEX_NAME_1 = "index_set_1_counts_test_0";
    private static final String INDEX_NAME_2 = "index_set_2_counts_test_0";
    private static final String INDEX_NAME_3 = "index_set_3_counts_test_0";

    private IndexSetRegistry indexSetRegistry;
    private IndexSet indexSet1;
    private IndexSet indexSet2;
    private IndexSet indexSet3;
    private Counts counts;
    private IndexSetConfig indexSetConfig1;
    private IndexSetConfig indexSetConfig2;
    private IndexSetConfig indexSetConfig3;

    public CountsIT(SearchServerInstance elasticsearch) {
        super(elasticsearch);
    }

    @BeforeAll
    public void setUp() {
        // @TestInstance(PER_CLASS) and the MockitoExtension don't work together, initialization has to be done manually
        indexSetRegistry = mock(IndexSetRegistry.class);
        indexSet1 = mock(IndexSet.class);
        indexSet2 = mock(IndexSet.class);
        indexSet3 = mock(IndexSet.class);

        client().createIndex(INDEX_NAME_1, 1, 0);
        client().createIndex(INDEX_NAME_2, 1, 0);
        client().createIndex(INDEX_NAME_3, 1, 0);
        client().waitForGreenStatus(INDEX_NAME_1, INDEX_NAME_2, INDEX_NAME_3);

        counts = new Counts(indexSetRegistry, searchServer().adapters().countsAdapter());

        indexSetConfig1 = IndexSetConfig.builder()
                .id("id-1")
                .title("title-1")
                .indexPrefix("index_set_1_counts_test")
                .shards(1)
                .replicas(0)
                .rotationStrategyClass(MessageCountRotationStrategy.class.getCanonicalName())
                .rotationStrategy(MessageCountRotationStrategyConfig.createDefault())
                .retentionStrategyClass(DeletionRetentionStrategy.class.getCanonicalName())
                .retentionStrategy(DeletionRetentionStrategyConfig.createDefault())
                .creationDate(ZonedDateTime.of(2016, 10, 12, 0, 0, 0, 0, ZoneOffset.UTC))
                .indexAnalyzer("standard")
                .indexTemplateName("template-1")
                .indexOptimizationMaxNumSegments(1)
                .indexOptimizationDisabled(false)
                .build();

        indexSetConfig2 = IndexSetConfig.builder()
                .id("id-2")
                .title("title-2")
                .indexPrefix("index_set_2_counts_test")
                .shards(1)
                .replicas(0)
                .rotationStrategyClass(MessageCountRotationStrategy.class.getCanonicalName())
                .rotationStrategy(MessageCountRotationStrategyConfig.createDefault())
                .retentionStrategyClass(DeletionRetentionStrategy.class.getCanonicalName())
                .retentionStrategy(DeletionRetentionStrategyConfig.createDefault())
                .creationDate(ZonedDateTime.of(2016, 10, 13, 0, 0, 0, 0, ZoneOffset.UTC))
                .indexAnalyzer("standard")
                .indexTemplateName("template-2")
                .indexOptimizationMaxNumSegments(1)
                .indexOptimizationDisabled(false)
                .build();

        indexSetConfig3 = IndexSetConfig.builder()
                .id("id-3")
                .title("title-3")
                .indexPrefix("index_set_3_counts_test")
                .shards(1)
                .replicas(0)
                .rotationStrategyClass(MessageCountRotationStrategy.class.getCanonicalName())
                .rotationStrategy(MessageCountRotationStrategyConfig.createDefault())
                .retentionStrategyClass(DeletionRetentionStrategy.class.getCanonicalName())
                .retentionStrategy(DeletionRetentionStrategyConfig.createDefault())
                .creationDate(ZonedDateTime.of(2016, 10, 13, 0, 0, 0, 0, ZoneOffset.UTC))
                .indexAnalyzer("standard")
                .indexTemplateName("template-3")
                .indexOptimizationMaxNumSegments(1)
                .indexOptimizationDisabled(false)
                .build();
    }

    @BeforeEach
    public void initMocks() {
        when(indexSetRegistry.getManagedIndices()).thenReturn(new String[]{INDEX_NAME_1, INDEX_NAME_2});
        when(indexSetRegistry.get(indexSetConfig1.id())).thenReturn(Optional.of(indexSet1));
        when(indexSetRegistry.get(indexSetConfig2.id())).thenReturn(Optional.of(indexSet2));
        when(indexSetRegistry.get(indexSetConfig3.id())).thenReturn(Optional.of(indexSet3));
        when(indexSet1.getManagedIndices()).thenReturn(new String[]{INDEX_NAME_1});
        when(indexSet2.getManagedIndices()).thenReturn(new String[]{INDEX_NAME_2});
        when(indexSet3.getManagedIndices()).thenReturn(new String[]{INDEX_NAME_3});
    }

    @ContainerMatrixTest
    @Order(1)
    public void totalReturnsZeroWithEmptyIndex() {
        assertThat(counts.total()).isEqualTo(0L);
        assertThat(counts.total(indexSet1)).isEqualTo(0L);
        assertThat(counts.total(indexSet2)).isEqualTo(0L);
        assertThat(counts.total(indexSet3)).isEqualTo(0L);
    }

    @ContainerMatrixTest
    @Order(2)
    public void totalReturnsZeroWithNoIndices() {
        final BulkIndexRequest bulkIndexRequest = new BulkIndexRequest();
        for (int i = 0; i < 10; i++) {
            final Map<String, Object> source = ImmutableMap.of(
                    "foo", "bar",
                    "counter", i);
            bulkIndexRequest.addRequest(INDEX_NAME_3, source);
        }

        client().bulkIndex(bulkIndexRequest);

        // Simulate no indices for the second index set.
        when(indexSet2.getManagedIndices()).thenReturn(new String[0]);

        assertThat(counts.total(indexSet1)).isEqualTo(0L);
        assertThat(counts.total(indexSet2)).isEqualTo(0L);
        assertThat(counts.total(indexSet3)).isEqualTo(10L);

        // Simulate no indices for all index sets.
        when(indexSetRegistry.getManagedIndices()).thenReturn(new String[0]);

        assertThat(counts.total()).isEqualTo(0L);
    }

    @ContainerMatrixTest
    public void totalReturnsNumberOfMessages() {
        final BulkIndexRequest bulkIndexRequest = new BulkIndexRequest();

        final int count1 = 10;
        for (int i = 0; i < count1; i++) {
            final Map<String, Object> source = ImmutableMap.of(
                    "foo", "bar",
                    "counter", i);
            bulkIndexRequest.addRequest(INDEX_NAME_1, source);
        }

        final int count2 = 5;
        for (int i = 0; i < count2; i++) {
            final Map<String, Object> source = ImmutableMap.of(
                    "foo", "bar",
                    "counter", i);
            bulkIndexRequest.addRequest(INDEX_NAME_2, source);
        }

        client().bulkIndex(bulkIndexRequest);

        assertThat(counts.total()).isEqualTo(count1 + count2);
        assertThat(counts.total(indexSet1)).isEqualTo(count1);
        assertThat(counts.total(indexSet2)).isEqualTo(count2);
    }

    @ContainerMatrixTest
    public void totalThrowsElasticsearchExceptionIfIndexDoesNotExist() {
        final IndexSet indexSet = mock(IndexSet.class);
        when(indexSet.getManagedIndices()).thenReturn(new String[]{"does_not_exist"});

        try {
            counts.total(indexSet);
            fail("Expected IndexNotFoundException");
        } catch (IndexNotFoundException e) {
            final String expectedErrorDetail = "Index not found for query: does_not_exist. Try recalculating your index ranges.";
            assertThat(e)
                    .hasMessageStartingWith("Fetching message count failed for indices [does_not_exist]")
                    .hasMessageEndingWith(expectedErrorDetail)
                    .hasNoSuppressedExceptions();
            assertThat(e.getErrorDetails()).containsExactly(expectedErrorDetail);
        }
    }

    @ContainerMatrixTest
    public void totalSucceedsWithListOfIndicesLargerThan4Kilobytes() {
        final int numberOfIndices = 100;
        final String[] indexNames = new String[numberOfIndices];
        final String indexPrefix = "very_long_list_of_indices_0123456789_counts_it_";
        final IndexSet indexSet = mock(IndexSet.class);

        for (int i = 0; i < numberOfIndices; i++) {
            final String indexName = indexPrefix + i;
            client().createIndex(indexName);
            indexNames[i] = indexName;
        }

        when(indexSet.getManagedIndices()).thenReturn(indexNames);

        final String indicesString = String.join(",", indexNames);
        assertThat(indicesString.length()).isGreaterThanOrEqualTo(4096);

        assertThat(counts.total(indexSet)).isEqualTo(0L);
    }
}
