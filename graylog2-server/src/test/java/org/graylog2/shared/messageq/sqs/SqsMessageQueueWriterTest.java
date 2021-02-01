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
package org.graylog2.shared.messageq.sqs;

import com.codahale.metrics.MetricRegistry;
import org.apache.commons.lang3.StringUtils;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.shared.buffers.RawMessageEvent;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.shared.messageq.sqs.SqsMessageQueueWriter.REQUEST_SIZE_LIMIT;
import static org.mockito.Mockito.when;

public class SqsMessageQueueWriterTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private MetricRegistry metricRegistry;
    @Mock
    private BaseConfiguration config;

    @Before
    public void setUp() throws Exception {
        when(config.getSqsQueueUrl()).thenReturn(new URI("http://localhost"));
    }

    @Test
    public void maxNumOfMessagesInBatch() {
        final SqsMessageQueueWriter writer = new SqsMessageQueueWriter(metricRegistry, config);
        final List<SqsMessageQueueWriter.Batch> batches = writer.createBatches(createEvents(22, "x"));
        assertThat(batches).hasSize(3);
        assertThat(batches.get(0).entries()).hasSize(10);
        assertThat(batches.get(1).entries()).hasSize(10);
        assertThat(batches.get(2).entries()).hasSize(2);
    }

    @Test
    public void maxBytesInBatch() {
        // make sure that only two messages will fit into one batch (after base64 encoding).
        String payload = StringUtils.repeat("x", REQUEST_SIZE_LIMIT/3);

        final SqsMessageQueueWriter writer = new SqsMessageQueueWriter(metricRegistry, config);
        final List<SqsMessageQueueWriter.Batch> batches =
                writer.createBatches(createEvents(4, payload));
        assertThat(batches).hasSize(2);
        assertThat(batches.get(0).entries()).hasSize(2);
        assertThat(batches.get(1).entries()).hasSize(2);
    }

    private List<RawMessageEvent> createEvents(int messageCount, String body) {
        return IntStream.range(0, messageCount)
                .mapToObj(i -> createEvent(body))
                .collect(Collectors.toList());
    }

    private RawMessageEvent createEvent(String body) {
        final RawMessageEvent rawMessageEvent = new RawMessageEvent();
        rawMessageEvent.setEncodedRawMessage(body.getBytes(StandardCharsets.UTF_8));
        return rawMessageEvent;
    }

}
