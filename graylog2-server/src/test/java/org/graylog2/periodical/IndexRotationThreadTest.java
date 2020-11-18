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
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.NoTargetIndexException;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.system.activities.NullActivityWriter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.inject.Provider;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IndexRotationThreadTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

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
    private NodeId nodeId;
    @Mock
    private IndexSetRegistry indexSetRegistry;

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
                ImmutableMap.<String, Provider<RotationStrategy>>builder().put("strategy", provider).build()
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
                ImmutableMap.<String, Provider<RotationStrategy>>builder().put("strategy", provider).build()
        );
        when(indexSetConfig.rotationStrategyClass()).thenReturn("strategy");

        rotationThread.checkForRotation(indexSet);

        verify(indexSet, never()).cycle();
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
                ImmutableMap.<String, Provider<RotationStrategy>>builder().put("strategy", provider).build()
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
            };
        }

        public void doRotate(IndexSet indexSet) {
        }
    }
}
