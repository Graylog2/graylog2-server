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
package org.graylog2.indexer.datanode;

import jakarta.annotation.Nonnull;
import org.assertj.core.api.Assertions;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.cluster.lock.Lock;
import org.graylog2.cluster.lock.MongoLockService;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.plugin.system.SimpleNodeId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

class DatanodeMigrationLockServiceImplTest {

    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @BeforeEach
    void setUp() {
        mongodb.start();
    }

    @AfterEach
    void tearDown() {
        mongodb.close();
    }



    @Test
    void testLockingTwoCallers() {
        final MongoLockService lockService = new MongoLockService(new SimpleNodeId("5ca1ab1e-0000-4000-a000-000000000000"), mongodb.mongoConnection(), Duration.ofSeconds(5));
        final DatanodeMigrationLockServiceImpl datanodeMigrationLockService = new DatanodeMigrationLockServiceImpl(lockService);

        final IndexSet indexSet = mockIndexSet("set-A");
        final Lock lock = datanodeMigrationLockService.acquireLock(indexSet, CallerA.class, "", new DatanodeMigrationLockWaitConfig(Duration.ofSeconds(1), Duration.ofSeconds(10), (indexSet1, caller, attemptNumber) -> {}));
        Assertions.assertThat(lock).isNotNull();

        final AtomicBoolean executed = new AtomicBoolean(false);
        datanodeMigrationLockService.tryRun(indexSet, CallerB.class, () -> executed.set(true));

        Assertions.assertThat(executed.get()).isFalse();

        datanodeMigrationLockService.release(lock);
        datanodeMigrationLockService.tryRun(indexSet, CallerB.class, () -> executed.set(true));

        Assertions.assertThat(executed.get()).isTrue();
    }

    @Test
    void testOneCallerTwoTasks() {
        final MongoLockService lockService = new MongoLockService(new SimpleNodeId("5ca1ab1e-0000-4000-a000-000000000000"), mongodb.mongoConnection(), Duration.ofSeconds(5));
        final DatanodeMigrationLockServiceImpl datanodeMigrationLockService = new DatanodeMigrationLockServiceImpl(lockService);

        final IndexSet indexSet = mockIndexSet("set-A");
        final Lock lock1 = datanodeMigrationLockService.acquireLock(indexSet, CallerA.class, "migration-1", new DatanodeMigrationLockWaitConfig(Duration.ofMillis(100), Duration.ofMillis(500), (indexSet1, caller, attemptNumber) -> {}));
        Assertions.assertThat(lock1).isNotNull();

        Assertions.assertThatThrownBy(() -> datanodeMigrationLockService.acquireLock(indexSet, CallerA.class, "migration-2", new DatanodeMigrationLockWaitConfig(Duration.ofMillis(100), Duration.ofMillis(500), (indexSet1, caller, attemptNumber) -> {})))
                .isInstanceOf(DatanodeMigrationLockException.class);
    }

    @Nonnull
    private IndexSet mockIndexSet(String title) {
        final IndexSet indexSet = Mockito.mock(IndexSet.class);
        final IndexSetConfig config = Mockito.mock(IndexSetConfig.class);
        Mockito.when(config.title()).thenReturn(title);
        Mockito.when(config.id()).thenReturn(title);
        Mockito.when(indexSet.getConfig()).thenReturn(config);
        return indexSet;
    }

    private class CallerA {};
    private class CallerB {};
}
