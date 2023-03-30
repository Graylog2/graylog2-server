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
package org.graylog2.indexer.fieldtypes;

import com.github.joschi.jadconfig.util.Duration;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog2.indexer.MongoIndexSet;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IndexFieldTypePollerPeriodicalTest {
    private IndexFieldTypePollerPeriodical periodical;
    private final IndexFieldTypePoller indexFieldTypePoller = mock(IndexFieldTypePoller.class);
    private final IndexFieldTypesService indexFieldTypesService = mock(IndexFieldTypesService.class);
    private final IndexSetService indexSetService = mock(IndexSetService.class);
    private final Indices indices = mock(Indices.class);
    private final MongoIndexSet.Factory mongoIndexSetFactory = mock(MongoIndexSet.Factory.class);
    private final Cluster cluster = mock(Cluster.class);
    @SuppressWarnings("UnstableApiUsage")
    private final EventBus eventBus = mock(EventBus.class);
    private final ServerStatus serverStatus = mock(ServerStatus.class);
    private final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(2,
            new ThreadFactoryBuilder().setNameFormat("index-field-type-poller-periodical-test-%d").build()
    );

    @BeforeEach
    void setUp() {
        this.periodical = new IndexFieldTypePollerPeriodical(indexFieldTypePoller,
                indexFieldTypesService,
                indexSetService,
                indices,
                mongoIndexSetFactory,
                cluster,
                eventBus,
                serverStatus,
                Duration.seconds(0),
                scheduler);
        when(serverStatus.getLifecycle()).thenReturn(Lifecycle.RUNNING);
        when(cluster.isConnected()).thenReturn(true);
    }

    @Test
    void scheduledExecutionIsSkippedWhenServerIsNotRunning() {
        when(serverStatus.getLifecycle()).thenReturn(Lifecycle.HALTING);

        periodical.doRun();

        verifyNoInteractions(cluster);
    }

    @Test
    void noConcurrentPollingForFieldTypes() throws InterruptedException {
        final IndexSetConfig indexSet = IndexSetConfig.builder()
                .id("indexSet1")
                .title("Test Index Set")
                .indexPrefix("test")
                .shards(2)
                .creationDate(ZonedDateTime.now())
                .indexAnalyzer("standard")
                .indexTemplateName("test")
                .indexOptimizationMaxNumSegments(2048)
                .indexOptimizationDisabled(false)
                .fieldTypeRefreshInterval(org.joda.time.Duration.standardSeconds(1))
                .retentionStrategy(NoopRetentionStrategyConfig.createDefault())
                .rotationStrategy(MessageCountRotationStrategyConfig.createDefault())
                .replicas(1)
                .build();
        final List<IndexSetConfig> indexSets = List.of(indexSet);
        when(indexSetService.findAll()).thenReturn(indexSets);

        final MongoIndexSet mongoIndexSet = mock(MongoIndexSet.class);
        when(mongoIndexSet.getActiveWriteIndex()).thenReturn("test_0");
        when(mongoIndexSetFactory.create(eq(indexSet))).thenReturn(mongoIndexSet);

        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(1);
        when(indexFieldTypePoller.pollIndex(anyString(), anyString()))
                .thenAnswer((Answer<Optional<IndexFieldTypesDTO>>) invocationOnMock -> {
                    start.countDown();
                    done.await();
                    return Optional.empty();
                });

        periodical.doRun();

        // Wait until first job is waiting for index field type poller
        start.await();

        // Then start second job
        periodical.doRun();

        // Waiting for second job to complete
        await().atMost(1, TimeUnit.MINUTES).until(() -> scheduler.getCompletedTaskCount() == 1);

        // And release first job
        done.countDown();

        await().atMost(1, TimeUnit.MINUTES).until(() -> scheduler.getCompletedTaskCount() == 2);

        verify(indexFieldTypePoller, times(1)).pollIndex(anyString(), anyString());
    }
}
