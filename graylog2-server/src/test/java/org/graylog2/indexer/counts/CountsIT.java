/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer.counts;

import com.google.common.collect.ImmutableMap;
import io.searchbox.core.Bulk;
import io.searchbox.core.BulkResult;
import io.searchbox.core.Index;
import org.graylog.testing.elasticsearch.ElasticsearchBaseTest;
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CountsIT extends ElasticsearchBaseTest {
    private static final String INDEX_NAME_1 = "index_set_1_counts_test_0";
    private static final String INDEX_NAME_2 = "index_set_2_counts_test_0";
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private IndexSetRegistry indexSetRegistry;
    @Mock
    private IndexSet indexSet1;
    @Mock
    private IndexSet indexSet2;
    private Counts counts;

    @Before
    public void setUp() throws Exception {
        client().createIndex(INDEX_NAME_1, 1, 0);
        client().createIndex(INDEX_NAME_2, 1, 0);
        client().waitForGreenStatus(INDEX_NAME_1, INDEX_NAME_2);

        counts = new Counts(jestClient(), indexSetRegistry);

        final IndexSetConfig indexSetConfig1 = IndexSetConfig.builder()
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

        final IndexSetConfig indexSetConfig2 = IndexSetConfig.builder()
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

        when(indexSetRegistry.getManagedIndices()).thenReturn(new String[]{INDEX_NAME_1, INDEX_NAME_2});
        when(indexSetRegistry.get(indexSetConfig1.id())).thenReturn(Optional.of(indexSet1));
        when(indexSetRegistry.get(indexSetConfig2.id())).thenReturn(Optional.of(indexSet2));
        when(indexSet1.getManagedIndices()).thenReturn(new String[]{INDEX_NAME_1});
        when(indexSet2.getManagedIndices()).thenReturn(new String[]{INDEX_NAME_2});
    }

    @Test
    public void totalReturnsZeroWithEmptyIndex() {
        assertThat(counts.total()).isEqualTo(0L);
        assertThat(counts.total(indexSet1)).isEqualTo(0L);
        assertThat(counts.total(indexSet2)).isEqualTo(0L);
    }

    @Test
    public void totalReturnsZeroWithNoIndices() throws Exception {
        final Bulk.Builder bulkBuilder = new Bulk.Builder().refresh(true);
        for (int i = 0; i < 10; i++) {
            final Map<String, Object> source = ImmutableMap.of(
                    "foo", "bar",
                    "counter", i);
            final Index indexRequest = new Index.Builder(source)
                    .index(INDEX_NAME_1)
                    .type("test")
                    .refresh(true)
                    .build();
            bulkBuilder.addAction(indexRequest);
        }
        final BulkResult bulkResult = client().executeWithExpectedSuccess(bulkBuilder.build(), "failed to execute bulk request");

        assertThat(bulkResult.getFailedItems()).isEmpty();

        // Simulate no indices for the second index set.
        when(indexSet2.getManagedIndices()).thenReturn(new String[0]);

        assertThat(counts.total(indexSet1)).isEqualTo(10L);
        assertThat(counts.total(indexSet2)).isEqualTo(0L);

        // Simulate no indices for all index sets.
        when(indexSetRegistry.getManagedIndices()).thenReturn(new String[0]);

        assertThat(counts.total()).isEqualTo(0L);
    }

    @Test
    public void totalReturnsNumberOfMessages() {
        final Bulk.Builder bulkBuilder = new Bulk.Builder().refresh(true);

        final int count1 = 10;
        for (int i = 0; i < count1; i++) {
            final Map<String, Object> source = ImmutableMap.of(
                    "foo", "bar",
                    "counter", i);
            final Index indexRequest = new Index.Builder(source)
                    .index(INDEX_NAME_1)
                    .type("test")
                    .refresh(true)
                    .build();
            bulkBuilder.addAction(indexRequest);
        }

        final int count2 = 5;
        for (int i = 0; i < count2; i++) {
            final Map<String, Object> source = ImmutableMap.of(
                    "foo", "bar",
                    "counter", i);
            final Index indexRequest = new Index.Builder(source)
                    .index(INDEX_NAME_2)
                    .type("test")
                    .refresh(true)
                    .build();
            bulkBuilder.addAction(indexRequest);
        }

        final BulkResult bulkResult = client().executeWithExpectedSuccess(bulkBuilder.build(), "failed to execute bulk request");

        assertThat(bulkResult.getFailedItems()).isEmpty();

        assertThat(counts.total()).isEqualTo(count1 + count2);
        assertThat(counts.total(indexSet1)).isEqualTo(count1);
        assertThat(counts.total(indexSet2)).isEqualTo(count2);
    }

    @Test
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

    @Test
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
