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

import org.graylog2.audit.AuditEventSender;
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.rotation.common.IndexRotator;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.system.SimpleNodeId;
import jakarta.validation.constraints.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MessageCountRotationStrategyTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();
    private final NodeId nodeId = new SimpleNodeId("5ca1ab1e-0000-4000-a000-000000000000");
    @Mock
    private IndexSet indexSet;
    @Mock
    private IndexSetConfig indexSetConfig;
    @Mock
    private Indices indices;
    @Mock
    private AuditEventSender auditEventSender;

    @Test
    public void testRotate() {
        when(indices.numberOfMessages("name")).thenReturn(10L);
        when(indexSet.getNewestIndex()).thenReturn("name");
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSetConfig.rotationStrategy()).thenReturn(MessageCountRotationStrategyConfig.create(5));

        final MessageCountRotationStrategy strategy = createStrategy();

        strategy.rotate(indexSet);
        verify(indexSet, times(1)).cycle();
        reset(indexSet);
    }

    @Test
    public void testDontRotate() {
        when(indices.numberOfMessages("name")).thenReturn(1L);
        when(indexSet.getNewestIndex()).thenReturn("name");
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSetConfig.rotationStrategy()).thenReturn(MessageCountRotationStrategyConfig.create(5));

        final MessageCountRotationStrategy strategy = createStrategy();

        strategy.rotate(indexSet);
        verify(indexSet, never()).cycle();
        reset(indexSet);
    }


    @Test
    public void testIndexUnavailable() {
        doThrow(IndexNotFoundException.class).when(indices).numberOfMessages("name");
        when(indexSet.getNewestIndex()).thenReturn("name");
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSetConfig.rotationStrategy()).thenReturn(MessageCountRotationStrategyConfig.create(5));

        final MessageCountRotationStrategy strategy = createStrategy();

        strategy.rotate(indexSet);
        verify(indexSet, never()).cycle();
        reset(indexSet);
    }

    @NotNull
    private MessageCountRotationStrategy createStrategy() {
        return new MessageCountRotationStrategy(indices, new IndexRotator(indices, auditEventSender, nodeId));
    }
}
