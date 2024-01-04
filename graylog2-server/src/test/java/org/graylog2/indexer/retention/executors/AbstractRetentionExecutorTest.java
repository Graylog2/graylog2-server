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
package org.graylog2.indexer.retention.executors;

import org.graylog.events.JobSchedulerTestClock;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.datatiering.hotonly.HotOnlyDataTieringConfig;
import org.graylog2.indexer.RetentionTestIndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.blocks.IndicesBlockStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class AbstractRetentionExecutorTest {

    final DateTime NOW = DateTime.now(DateTimeZone.UTC);

    JobSchedulerTestClock clock;

    RetentionTestIndexSet indexSet;

    @Mock
    Indices indices;
    @Mock
    ActivityWriter activityWriter;

    @Mock
    RetentionExecutor.RetentionAction action;
    @Captor
    ArgumentCaptor<List<String>> retainedIndexName;

    RetentionExecutor retentionExecutor;

    public void setUp() throws Exception {
        clock = new JobSchedulerTestClock(Tools.nowUTC());

        var elasticsearchConfiguration = new ElasticsearchConfiguration();
        indexSet = new RetentionTestIndexSet(IndexSetConfig.builder()
                .title("test index")
                .indexPrefix("test")
                .shards(1)
                .replicas(1)
                .creationDate(clock.now(ZoneOffset.UTC))
                .indexAnalyzer(elasticsearchConfiguration.getAnalyzer())
                .indexTemplateName(elasticsearchConfiguration.getDefaultIndexTemplateName())
                .indexOptimizationMaxNumSegments(elasticsearchConfiguration.getIndexOptimizationMaxNumSegments())
                .indexOptimizationDisabled(elasticsearchConfiguration.isDisableIndexOptimization())
                .rotationStrategy(null)
                .retentionStrategy(null)
                .dataTiering(HotOnlyDataTieringConfig.builder()
                        .indexLifetimeMin(elasticsearchConfiguration.getTimeSizeOptimizingRetentionMinLifeTime())
                        .indexLifetimeMax(elasticsearchConfiguration.getTimeSizeOptimizingRetentionMaxLifeTime())
                        .build())
                .build(), clock);

        lenient().when(indices.indexCreationDate(anyString())).thenAnswer(a -> {
            final String indexName = a.getArgument(0);
            final Optional<RetentionTestIndexSet.TestIndex> index = indexSet.findByName(indexName);
            return index.map(RetentionTestIndexSet.TestIndex::getCreationDate);
        });
        lenient().when(indices.indexClosingDate(anyString())).then(a -> {
            final String indexName = a.getArgument(0);
            final Optional<RetentionTestIndexSet.TestIndex> index = indexSet.findByName(indexName);
            return index.map(RetentionTestIndexSet.TestIndex::getClosingDate);
        });

        indexSet.addNewIndex(1, 123);
        clock.plus(4, TimeUnit.DAYS);
        indexSet.cycle();
        clock.plus(1, TimeUnit.DAYS);
        indexSet.cycle();
        clock.plus(1, TimeUnit.DAYS);
        indexSet.cycle();
        clock.plus(6, TimeUnit.DAYS);
        indexSet.cycle();
        clock.plus(1, TimeUnit.DAYS);
        indexSet.cycle();
        clock.plus(4, TimeUnit.DAYS);

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
        retentionExecutor = new RetentionExecutor(activityWriter, indices);
    }
}
