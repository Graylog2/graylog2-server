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
package org.graylog2.indexer.rotation.strategies;

import org.graylog.events.JobSchedulerTestClock;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.RetentionTestIndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.blocks.IndicesBlockStatus;
import org.graylog2.indexer.retention.executors.RetentionExecutor;
import org.graylog2.indexer.retention.executors.TimeBasedRetentionExecutor;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig;
import org.graylog2.indexer.rotation.common.IndexRotator;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.joda.time.Period;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.shared.utilities.StringUtils.f;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TimeBasedSizeOptimizingRotationAndRetentionTest {
    private static final Logger LOG = LoggerFactory.getLogger(TimeBasedSizeOptimizingRotationAndRetentionTest.class);

    private TimeBasedSizeOptimizingStrategy timeBasedSizeOptimizingStrategy;

    @Mock
    private Indices indices;

    @Mock
    private NodeId nodeId;

    @Mock
    private AuditEventSender auditEventSender;

    @Mock
    private ActivityWriter activityWriter;

    private JobSchedulerTestClock clock;

    private RetentionTestIndexSet indexSet;
    private ElasticsearchConfiguration elasticsearchConfiguration;
    private TimeBasedSizeOptimizingStrategyConfig rotationStrategyConfig;

    private DeletionRetentionStrategy deletionRetentionStrategy;

    @BeforeEach
    void setUp() {
        clock = new JobSchedulerTestClock(Tools.nowUTC());

        elasticsearchConfiguration = new ElasticsearchConfiguration();
        IndexRotator indexRotator = new IndexRotator(indices, auditEventSender, nodeId);
        timeBasedSizeOptimizingStrategy = new TimeBasedSizeOptimizingStrategy(indices, elasticsearchConfiguration, clock, indexRotator);
        rotationStrategyConfig = TimeBasedSizeOptimizingStrategyConfig.builder()
                .indexLifetimeMin(Period.days(4))
                .indexLifetimeMax(Period.days(6))
                .build();

        final DeletionRetentionStrategyConfig deletionRetention = DeletionRetentionStrategyConfig.createDefault();
        RetentionExecutor retentionExecutor = new RetentionExecutor(activityWriter, indices);
        deletionRetentionStrategy = new DeletionRetentionStrategy(
                indices,
                nodeId,
                auditEventSender,
                null,
                new TimeBasedRetentionExecutor(indices, clock, activityWriter, retentionExecutor));

        indexSet = new RetentionTestIndexSet(IndexSetConfig.builder()
                .title("test index")
                .indexPrefix("test")
                .shards(elasticsearchConfiguration.getShards())
                .replicas(elasticsearchConfiguration.getReplicas())
                .creationDate(clock.now(ZoneOffset.UTC))
                .indexAnalyzer(elasticsearchConfiguration.getAnalyzer())
                .indexTemplateName(elasticsearchConfiguration.getDefaultIndexTemplateName())
                .indexOptimizationMaxNumSegments(elasticsearchConfiguration.getIndexOptimizationMaxNumSegments())
                .indexOptimizationDisabled(elasticsearchConfiguration.isDisableIndexOptimization())
                .rotationStrategy(rotationStrategyConfig)
                .retentionStrategy(deletionRetention)
                .build(), clock);

        lenient().when(indices.indexCreationDate(anyString())).thenAnswer(a -> {
            final String indexName = a.getArgument(0);
            final Optional<RetentionTestIndexSet.TestIndex> index = indexSet.findByName(indexName);
            return index.map(RetentionTestIndexSet.TestIndex::getCreationDate);
        });

        lenient().when(indices.getStoreSizeInBytes(anyString())).then(a -> {
            final String indexName = a.getArgument(0);
            final Optional<RetentionTestIndexSet.TestIndex> index = indexSet.findByName(indexName);
            return index.map(RetentionTestIndexSet.TestIndex::getSize);
        });

        lenient().when(indices.numberOfMessages(anyString())).thenReturn(10L);

        lenient().when(indices.indexClosingDate(anyString())).then(a -> {
            final String indexName = a.getArgument(0);
            final Optional<RetentionTestIndexSet.TestIndex> index = indexSet.findByName(indexName);
            return index.map(RetentionTestIndexSet.TestIndex::getClosingDate);
        });

        // Report all indices that have a closingDate as read-only
        lenient().when(indices.getIndicesBlocksStatus(anyList())).then(a -> {
            final List<String> indices = a.getArgument(0);
            final IndicesBlockStatus indicesBlockStatus = new IndicesBlockStatus();
            indices.forEach(i -> {
                final Optional<RetentionTestIndexSet.TestIndex> index = indexSet.findByName(i);
                if (index.map(RetentionTestIndexSet.TestIndex::getClosingDate).orElse(null) != null) {
                    indicesBlockStatus.addIndexBlocks(i, Set.of("index.blocks.write"));
                }
            });
            return indicesBlockStatus;
        });

        lenient().doAnswer(a -> {
            final String indexName = a.getArgument(0);
            indexSet.deleteByName(indexName);
            return null;
        }).when(indices).delete(anyString());
    }

    @Test
    void rotationAndThenRetention() {
        testRotation();
        testRetention();
    }

    void testRotation() {
        indexSet.addNewIndex(0, elasticsearchConfiguration.getTimeSizeOptimizingRotationMinShardSize().toBytes());
        assertThat(indexSet.getIndicesNames()).isEqualTo(List.of("test_0"));

        // We rotate _after_ 1 day. This is too early
        clock.plus(12, TimeUnit.HOURS);
        timeBasedSizeOptimizingStrategy.rotate(indexSet);
        assertThat(indexSet.getIndicesNames()).isEqualTo(List.of("test_0"));

        // We rotate _after_ 1 day. Now is the time.
        clock.plus(12, TimeUnit.HOURS);
        timeBasedSizeOptimizingStrategy.rotate(indexSet);
        assertThat(indexSet.getIndicesNames()).isEqualTo(List.of("test_0", "test_1"));

        // We can skp the rotation if the index is not big enough.
        clock.plus(1, TimeUnit.DAYS);
        timeBasedSizeOptimizingStrategy.rotate(indexSet);
        assertThat(indexSet.getIndicesNames()).isEqualTo(List.of("test_0", "test_1"));

        // with the minimum required size, this works
        indexSet.getNewest().setSize(elasticsearchConfiguration.getTimeSizeOptimizingRotationMinShardSize().toBytes());
        clock.plus(1, TimeUnit.DAYS);
        timeBasedSizeOptimizingStrategy.rotate(indexSet);
        assertThat(indexSet.getIndicesNames()).isEqualTo(List.of("test_0", "test_1", "test_2"));

        // If an index exceeds its maximum size
        // it can be rotated, even if the rotation period has been reached.
        indexSet.getNewest().setSize(elasticsearchConfiguration.getTimeSizeOptimizingRotationMaxShardSize().toBytes() + 1);
        clock.plus(12, TimeUnit.HOURS);
        timeBasedSizeOptimizingStrategy.rotate(indexSet);
        assertThat(indexSet.getIndicesNames()).isEqualTo(List.of("test_0", "test_1", "test_2", "test_3"));

        // If an index doesn't reach its minimum size
        // but exceeds the optimization "leeway" ( indexLifetimeHard - indexLifetimeSoft) (6 - 4) = 2 days
        // it will also be rotated.
        indexSet.getNewest().setSize(100);
        clock.plus(2, TimeUnit.DAYS);
        timeBasedSizeOptimizingStrategy.rotate(indexSet);
        assertThat(indexSet.getIndicesNames()).isEqualTo(List.of("test_0", "test_1", "test_2", "test_3", "test_4"));
    }

    void testRetention() {
        assertThat(indexSet.getIndicesNames()).isEqualTo(List.of("test_0", "test_1", "test_2", "test_3", "test_4"));

        LOG.info("Existing indices from rotation:\n<{}>",
                indexSet.getIndices().stream()
                        .map(i -> f("%s: created: %s closed: %s/(%s ago)\n", i.getName(), i.getCreationDate(), i.getClosingDate(), getBetween(i))).toList());

        // Retention should happen after indexLifetimeSoft (4 days)
        deletionRetentionStrategy.retain(indexSet);

        // Only test_0 will be retained
        assertThat(indexSet.getIndicesNames()).isEqualTo(List.of("test_1", "test_2", "test_3", "test_4"));

        // Moving forward in time will retain more indices
        clock.plus(36, TimeUnit.HOURS);
        deletionRetentionStrategy.retain(indexSet);
        assertThat(indexSet.getIndicesNames()).isEqualTo(List.of("test_2", "test_3", "test_4"));

        clock.plus(1, TimeUnit.DAYS);
        deletionRetentionStrategy.retain(indexSet);
        assertThat(indexSet.getIndicesNames()).isEqualTo(List.of("test_3", "test_4"));

        clock.plus(3, TimeUnit.DAYS);
        deletionRetentionStrategy.retain(indexSet);
        assertThat(indexSet.getIndicesNames()).isEqualTo(List.of("test_4"));

        // Active index will never be retained
        clock.plus(10, TimeUnit.DAYS);
        deletionRetentionStrategy.retain(indexSet);
        assertThat(indexSet.getIndicesNames()).isEqualTo(List.of("test_4"));
    }

    @Test
    public void testRetentionWithoutClosingDate() {
        // Report all but the newest index as read-only
        lenient().when(indices.getIndicesBlocksStatus(anyList())).then(a -> {
            final List<String> indices = a.getArgument(0);
            final IndicesBlockStatus indicesBlockStatus = new IndicesBlockStatus();
            final String newestIndex = indexSet.getNewestIndex();
            indices.forEach(i -> {
                if (!newestIndex.equals(i)) {
                    indicesBlockStatus.addIndexBlocks(i, Set.of("index.blocks.write"));
                }
            });
            return indicesBlockStatus;
        });

        indexSet.addNewIndex(0, elasticsearchConfiguration.getTimeSizeOptimizingRotationMinShardSize().toBytes());
        clock.plus(1, TimeUnit.DAYS);
        indexSet.addNewIndex(1, elasticsearchConfiguration.getTimeSizeOptimizingRotationMinShardSize().toBytes());
        clock.plus(1, TimeUnit.DAYS);
        indexSet.addNewIndex(2, elasticsearchConfiguration.getTimeSizeOptimizingRotationMinShardSize().toBytes());

        assertThat(indexSet.getIndicesNames()).isEqualTo(List.of("test_0", "test_1", "test_2"));

        clock.plus(2, TimeUnit.DAYS);

        // Retention without a closingDate should not happen after indexLifetimeSoft (4 days), but after indexLifetimeHard (6 days)
        deletionRetentionStrategy.retain(indexSet);
        assertThat(indexSet.getIndicesNames()).isEqualTo(List.of("test_0", "test_1", "test_2"));

        clock.plus(2, TimeUnit.DAYS);
        deletionRetentionStrategy.retain(indexSet);
        assertThat(indexSet.getIndicesNames()).isEqualTo(List.of("test_1", "test_2"));
    }

    private String getBetween(RetentionTestIndexSet.TestIndex i) {
        if (i.getClosingDate() == null) {
            return "null";
        }
        return Period.fieldDifference(clock.nowUTC().toLocalDateTime(), i.getClosingDate().toLocalDateTime()).toString();
    }

}
