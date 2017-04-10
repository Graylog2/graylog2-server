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
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.unit.TimeValue;
import org.graylog2.AbstractESTest;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.junit.After;
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
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CountsTest extends AbstractESTest {
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
    private IndexSetConfig indexSetConfig1;
    private IndexSetConfig indexSetConfig2;

    @Before
    public void setUp() throws Exception {
        final Map<String, Object> settings = ImmutableMap.of(
                "number_of_shards", 1,
                "index.number_of_replicas", 0);
        final CreateIndexResponse createIndexResponse1 = client.admin().indices()
                .prepareCreate(INDEX_NAME_1)
                .setSettings(settings)
                .setTimeout(TimeValue.timeValueSeconds(10L))
                .execute()
                .get();
        assumeTrue(createIndexResponse1.isAcknowledged());

        final CreateIndexResponse createIndexResponse2 = client.admin().indices()
                .prepareCreate(INDEX_NAME_2)
                .setSettings(settings)
                .setTimeout(TimeValue.timeValueSeconds(10L))
                .execute()
                .get();
        assumeTrue(createIndexResponse2.isAcknowledged());

        final ClusterHealthResponse clusterHealthResponse1 = client.admin().cluster()
                .prepareHealth(INDEX_NAME_1)
                .setWaitForGreenStatus()
                .execute()
                .get();
        assumeTrue(clusterHealthResponse1.getStatus() == ClusterHealthStatus.GREEN);

        final ClusterHealthResponse clusterHealthResponse2 = client.admin().cluster()
                .prepareHealth(INDEX_NAME_2)
                .setWaitForGreenStatus()
                .execute()
                .get();
        assumeTrue(clusterHealthResponse2.getStatus() == ClusterHealthStatus.GREEN);

        counts = new Counts(client, indexSetRegistry);

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

        when(indexSetRegistry.getManagedIndices()).thenReturn(new String[]{INDEX_NAME_1, INDEX_NAME_2});
        when(indexSetRegistry.get(indexSetConfig1.id())).thenReturn(Optional.of(indexSet1));
        when(indexSetRegistry.get(indexSetConfig2.id())).thenReturn(Optional.of(indexSet2));
        when(indexSet1.getManagedIndices()).thenReturn(new String[]{INDEX_NAME_1});
        when(indexSet2.getManagedIndices()).thenReturn(new String[]{INDEX_NAME_2});
    }

    @After
    public void tearDown() throws Exception {
        final DeleteIndexResponse deleteIndexResponse1 = client.admin().indices()
                .prepareDelete(INDEX_NAME_1)
                .setTimeout(TimeValue.timeValueSeconds(10L))
                .execute()
                .get();
        assumeTrue(deleteIndexResponse1.isAcknowledged());

        final DeleteIndexResponse deleteIndexResponse2 = client.admin().indices()
                .prepareDelete(INDEX_NAME_2)
                .setTimeout(TimeValue.timeValueSeconds(10L))
                .execute()
                .get();
        assumeTrue(deleteIndexResponse2.isAcknowledged());
    }

    @Test
    public void totalReturnsZeroWithEmptyIndex() throws Exception {
        assertThat(counts.total()).isEqualTo(0L);
        assertThat(counts.total(indexSet1)).isEqualTo(0L);
        assertThat(counts.total(indexSet2)).isEqualTo(0L);
    }

    @Test
    public void totalReturnsZeroWithNoIndices() throws Exception {
        for (int i = 0; i < 10; i++) {
            final IndexResponse indexResponse = client.prepareIndex()
                    .setIndex(INDEX_NAME_1)
                    .setRefresh(true)
                    .setType("test")
                    .setSource("foo", "bar", "counter", i)
                    .execute().get();
            assumeTrue(indexResponse.isCreated());
        }

        // Simulate no indices for the second index set.
        when(indexSet2.getManagedIndices()).thenReturn(new String[0]);

        assertThat(counts.total(indexSet1)).isEqualTo(10L);
        assertThat(counts.total(indexSet2)).isEqualTo(0L);

        // Simulate no indices for all index sets.
        when(indexSetRegistry.getManagedIndices()).thenReturn(new String[0]);

        assertThat(counts.total()).isEqualTo(0L);
    }

    @Test
    public void totalReturnsNumberOfMessages() throws Exception {
        final int count1 = 10;
        final int count2 = 5;
        for (int i = 0; i < count1; i++) {
            final IndexResponse indexResponse = client.prepareIndex()
                    .setIndex(INDEX_NAME_1)
                    .setRefresh(true)
                    .setType("test")
                    .setSource("foo", "bar", "counter", i)
                    .execute().get();
            assumeTrue(indexResponse.isCreated());
        }
        for (int i = 0; i < count2; i++) {
            final IndexResponse indexResponse = client.prepareIndex()
                    .setIndex(INDEX_NAME_2)
                    .setRefresh(true)
                    .setType("test")
                    .setSource("foo", "bar", "counter", i)
                    .execute().get();
            assumeTrue(indexResponse.isCreated());
        }

        assertThat(counts.total()).isEqualTo(count1 + count2);
        assertThat(counts.total(indexSet1)).isEqualTo(count1);
        assertThat(counts.total(indexSet2)).isEqualTo(count2);
    }

    @Test
    public void totalReturnsMinusOneIfIndexDoesNotExist() throws Exception {
        final IndexSet indexSet = mock(IndexSet.class);
        when(indexSet.getManagedIndices()).thenReturn(new String[]{"does_not_exist"});
        assertThat(counts.total(indexSet)).isEqualTo(-1L);
    }
}
