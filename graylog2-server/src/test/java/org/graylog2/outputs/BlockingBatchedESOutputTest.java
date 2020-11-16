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
package org.graylog2.outputs;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.graylog2.Configuration;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.shared.journal.NoopJournal;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class BlockingBatchedESOutputTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private MetricRegistry metricRegistry;
    private NoopJournal journal;
    private Configuration config;

    @Mock
    private Messages messages;

    @Before
    public void setUp() throws Exception {
        this.metricRegistry = new MetricRegistry();
        this.journal = new NoopJournal();
        this.config = new Configuration() {
            @Override
            public int getOutputBatchSize() {
                return 3;
            }
        };
    }

    @Test
    public void write() throws Exception {
        final BlockingBatchedESOutput output = new BlockingBatchedESOutput(metricRegistry, messages, config, journal);

        final List<Map.Entry<IndexSet, Message>> messageList = buildMessages(config.getOutputBatchSize());

        for (Map.Entry<IndexSet, Message> entry : messageList) {
            output.writeMessageEntry(entry);
        }

        verify(messages, times(1)).bulkIndex(eq(messageList));
    }

    @Test
    public void forceFlushIfTimedOut() throws Exception {
        final BlockingBatchedESOutput output = new BlockingBatchedESOutput(metricRegistry, messages, config, journal);

        final List<Map.Entry<IndexSet, Message>> messageList = buildMessages(config.getOutputBatchSize() - 1);

        for (Map.Entry<IndexSet, Message> entry : messageList) {
            output.writeMessageEntry(entry);
        }

        // Should flush the buffer even though the batch size is not reached yet
        output.forceFlushIfTimedout();

        verify(messages, times(1)).bulkIndex(eq(messageList));
    }

    private List<Map.Entry<IndexSet, Message>> buildMessages(final int count) {
        final ImmutableList.Builder<Map.Entry<IndexSet, Message>> builder = ImmutableList.builder();
        for (int i = 0; i < count; i++) {
            builder.add(Maps.immutableEntry(mock(IndexSet.class), new Message("message" + i, "test", Tools.nowUTC())));
        }

        return builder.build();
    }
}