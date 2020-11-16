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
import org.graylog2.indexer.indexset.events.IndexSetCreatedEvent;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.mockito.Mockito.mock;
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
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
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
                Duration.seconds(30),
                scheduler);
    }

    @Test
    void scheduledExecutionIsSkippedWhenServerIsNotRunning() throws InterruptedException {
        when(serverStatus.getLifecycle()).thenReturn(Lifecycle.HALTING);
        final IndexSetConfig indexSetConfig = mockIndexSetConfig();
        final IndexSetCreatedEvent indexSetCreatedEvent = IndexSetCreatedEvent.create(indexSetConfig);

        when(indexSetService.get("foo")).thenReturn(Optional.of(indexSetConfig));

        final MongoIndexSet mongoIndexSet = mockMongoIndexSet(indexSetConfig);
        when(mongoIndexSetFactory.create(indexSetConfig)).thenReturn(mongoIndexSet);

        this.periodical.handleIndexSetCreation(indexSetCreatedEvent);

        Thread.sleep(100);

        verifyNoInteractions(indexFieldTypePoller);
    }

    private MongoIndexSet mockMongoIndexSet(IndexSetConfig indexSetConfig) {
        final MongoIndexSet mongoIndexSet = mock(MongoIndexSet.class);
        when(mongoIndexSet.getConfig()).thenReturn(indexSetConfig);
        when(mongoIndexSet.getActiveWriteIndex()).thenReturn("foo-0");
        return mongoIndexSet;
    }

    private IndexSetConfig mockIndexSetConfig() {
        final IndexSetConfig indexSetConfig = mock(IndexSetConfig.class);
        when(indexSetConfig.id()).thenReturn("foo");
        when(indexSetConfig.fieldTypeRefreshInterval()).thenReturn(new org.joda.time.Duration(1L));
        when(indexSetConfig.isWritable()).thenReturn(true);
        return indexSetConfig;
    }
}
