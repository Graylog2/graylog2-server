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
package org.graylog2.periodical;

import com.google.common.collect.ImmutableMap;
import jakarta.annotation.Nonnull;
import jakarta.inject.Provider;
import org.assertj.core.api.Assertions;
import org.graylog2.cluster.lock.Lock;
import org.graylog2.datatiering.DataTieringOrchestrator;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.NoTargetIndexException;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.datanode.DatanodeMigrationLockService;
import org.graylog2.indexer.datanode.DatanodeMigrationLockServiceImpl;
import org.graylog2.indexer.datanode.DatanodeMigrationLockWaitConfig;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.shared.system.activities.NullActivityWriter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IndexRotationThreadTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();
    private final NodeId nodeId = new SimpleNodeId("5ca1ab1e-0000-4000-a000-000000000000");
    @Mock
    private IndexSet indexSet;
    @Mock
    private IndexSetConfig indexSetConfig;
    @Mock
    private NotificationService notificationService;
    @Mock
    private Indices indices;
    @Mock
    private Cluster cluster;
    @Mock
    private IndexSetRegistry indexSetRegistry;
    @Mock
    private DataTieringOrchestrator dataTieringOrchestrator;

    @Before
    public void setUp() throws Exception {
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
    }

    @Test
    public void testPerformRotation() throws NoTargetIndexException {
        final Provider<RotationStrategy> provider = new RotationStrategyProvider() {
            @Override
            public void doRotate(IndexSet indexSet) {
                indexSet.cycle();
            }
        };

        final IndexRotationThread rotationThread = new IndexRotationThread(
                notificationService,
                indices,
                indexSetRegistry,
                cluster,
                new NullActivityWriter(),
                nodeId,
                ImmutableMap.<String, Provider<RotationStrategy>>builder().put("strategy", provider).build(),
                dataTieringOrchestrator,
                Mockito.mock(DatanodeMigrationLockServiceImpl.class)
        );
        when(indexSetConfig.rotationStrategyClass()).thenReturn("strategy");

        rotationThread.checkForRotation(indexSet);

        verify(indexSet, times(1)).cycle();
    }

    @Test
    public void testDoNotPerformRotation() throws NoTargetIndexException {
        final Provider<RotationStrategy> provider = new RotationStrategyProvider();

        final IndexRotationThread rotationThread = new IndexRotationThread(
                notificationService,
                indices,
                indexSetRegistry,
                cluster,
                new NullActivityWriter(),
                nodeId,
                ImmutableMap.<String, Provider<RotationStrategy>>builder().put("strategy", provider).build(),
                dataTieringOrchestrator,
                Mockito.mock(DatanodeMigrationLockService.class)
        );
        when(indexSetConfig.rotationStrategyClass()).thenReturn("strategy");

        rotationThread.checkForRotation(indexSet);

        verify(indexSet, never()).cycle();
    }

    @Test
    public void testSkipRotationDuringMigration() throws NoTargetIndexException {
        TestableRotationStrategy testableRotationStrategy = new TestableRotationStrategy();
        final IndexSet indexSetFinished = mockIndexSet("finished_index_set", true, testableRotationStrategy);
        final IndexSet indexSetMigrated = mockIndexSet("migrating_index_set", true, testableRotationStrategy);

        final IndexRotationThread rotationThread = new IndexRotationThread(
                Mockito.mock(NotificationService.class),
                Mockito.mock(Indices.class),
                mockIndexSetRegistry(indexSetFinished, indexSetMigrated),
                mockCluster(true),
                new NullActivityWriter(),
                new SimpleNodeId("5ca1ab1e-0000-4000-a000-000000000000"),
                testableRotationStrategy.toProviderMap(),
                Mockito.mock(DataTieringOrchestrator.class),
                mockMigrationLocks(indexSetMigrated)
        );
        rotationThread.doRun();

        Assertions.assertThat(testableRotationStrategy.getRotatedIndices())
                .contains(indexSetFinished)
                .doesNotContain(indexSetMigrated);
    }

    @Nonnull
    private DatanodeMigrationLockService mockMigrationLocks(IndexSet lockedIndexSet) {
        return new DatanodeMigrationLockService() {
            @Override
            public Lock acquireLock(IndexSet indexSet, Class<?> caller, String context, DatanodeMigrationLockWaitConfig config) {
                return null;
            }

            @Override
            public void tryRun(IndexSet indexSet, Class<?> caller, Runnable runnable) {
                if(indexSet != lockedIndexSet) {
                    runnable.run();
                }
            }

            @Override
            public void release(Lock lock) {

            }
        };
    }

    private IndexSet mockIndexSet(String indexSetTitle, boolean writable, RotationStrategy testableRotationStrategy) {
        final IndexSet indexSet = Mockito.mock(IndexSet.class);
        final IndexSetConfig config = Mockito.mock(IndexSetConfig.class);
        Mockito.when(config.isWritable()).thenReturn(writable);
        Mockito.when(config.rotationStrategyClass()).thenReturn(testableRotationStrategy.getStrategyName());
        Mockito.when(config.title()).thenReturn(indexSetTitle);
        Mockito.when(config.id()).thenReturn(indexSetTitle);
        Mockito.when(indexSet.getConfig()).thenReturn(config);
        return indexSet;
    }

    private Cluster mockCluster(boolean connected) {
        final Cluster cluster = Mockito.mock(Cluster.class);
        Mockito.when(cluster.isConnected()).thenReturn(connected);
        return cluster;
    }

    @Nonnull
    private IndexSetRegistry mockIndexSetRegistry(IndexSet... indexSets) {
        final IndexSetRegistry registry = Mockito.mock(IndexSetRegistry.class);
        final List<IndexSet> sets = Arrays.asList(indexSets);

        // mock the Iterable forEach implementation :-/
        Mockito.doAnswer(invocation -> {
            Consumer<IndexSet> action = invocation.getArgument(0);
            sets.forEach(action);
            return null;
        }).when(registry).forEach(Mockito.any());

        return registry;
    }

    @Test
    public void testDoNotPerformRotationIfClusterIsDown() throws NoTargetIndexException {
        final Provider<RotationStrategy> provider = spy(new RotationStrategyProvider());
        when(cluster.isConnected()).thenReturn(false);

        final IndexRotationThread rotationThread = new IndexRotationThread(
                notificationService,
                indices,
                indexSetRegistry,
                cluster,
                new NullActivityWriter(),
                nodeId,
                ImmutableMap.<String, Provider<RotationStrategy>>builder().put("strategy", provider).build(),
                dataTieringOrchestrator,
                Mockito.mock(DatanodeMigrationLockServiceImpl.class)
        );
        rotationThread.doRun();

        verify(indexSet, never()).cycle();
        verify(provider, never()).get();
    }

    private static class RotationStrategyProvider implements Provider<RotationStrategy> {
        @Override
        public RotationStrategy get() {
            return new RotationStrategy() {
                @Override
                public void rotate(IndexSet indexSet) {
                    doRotate(indexSet);
                }

                @Override
                public RotationStrategyConfig defaultConfiguration() {
                    return null;
                }

                @Override
                public Class<? extends RotationStrategyConfig> configurationClass() {
                    return null;
                }

                @Override
                public String getStrategyName() {
                    return null;
                }
            };
        }

        public void doRotate(IndexSet indexSet) {
        }
    }
}
