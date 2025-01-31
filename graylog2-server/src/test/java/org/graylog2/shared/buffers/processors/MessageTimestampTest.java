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
package org.graylog2.shared.buffers.processors;

import com.codahale.metrics.MetricRegistry;
import com.google.common.eventbus.EventBus;
import jakarta.inject.Provider;
import org.graylog.failure.FailureSubmissionService;
import org.graylog2.buffers.OutputBuffer;
import org.graylog2.messageprocessors.OrderedMessageProcessors;
import org.graylog2.plugin.InstantMillisProvider;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.buffers.MessageEvent;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamMetrics;
import org.graylog2.system.processing.ProcessingStatusRecorder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class MessageTimestampTest {
    static final int GRACE_PERIOD_DAYS = 10;
    final MessageFactory messageFactory = new TestMessageFactory();
    ProcessBufferProcessor processBufferProcessor;
    DateTime initialTime;

    @BeforeEach
    void setUp() {
        initialTime = new DateTime(2014, 1, 1, 1, 59, 59, 0, DateTimeZone.UTC);
        final InstantMillisProvider clock = new InstantMillisProvider(initialTime);
        DateTimeUtils.setCurrentMillisProvider(clock);

        processBufferProcessor = createProcessor(Duration.ofDays(GRACE_PERIOD_DAYS));
    }

    @Test
    void testNoNormalization() throws Exception {
        DateTime time = initialTime.plusDays(GRACE_PERIOD_DAYS - 1);
        Message msg = messageFactory.createMessage("test message", "localhost", time);
        MessageEvent event = new MessageEvent();
        event.setMessage(msg);

        processBufferProcessor.onEvent(event);

        assertThat(msg.getTimestamp()).isEqualTo(time);
    }

    @Test
    void testNormalizeToNow() throws Exception {
        DateTime time = initialTime.plusDays(GRACE_PERIOD_DAYS + 1);
        Message msg = messageFactory.createMessage("test message", "localhost", time);
        MessageEvent event = new MessageEvent();
        event.setMessage(msg);

        processBufferProcessor.onEvent(event);

        assertThat(msg.getTimestamp()).isEqualTo(initialTime);
    }

    @Test
    void testNormalizeToReceiveTime() throws Exception {
        DateTime time = initialTime.plusDays(GRACE_PERIOD_DAYS + 1);
        Message msg = messageFactory.createMessage("test message", "localhost", time);
        DateTime receiveTime = initialTime.plusDays(GRACE_PERIOD_DAYS - 1);
        msg.setReceiveTime(receiveTime);
        MessageEvent event = new MessageEvent();
        event.setMessage(msg);

        processBufferProcessor.onEvent(event);

        assertThat(msg.getTimestamp()).isEqualTo(receiveTime);
    }

    ProcessBufferProcessor createProcessor(Duration gracePeriod) {
        MetricRegistry metricRegistry = new MetricRegistry();
        StreamMetrics streamMetrics = new StreamMetrics(metricRegistry);

        Provider<Stream> defaultStreamProvider = Mockito.mock(Provider.class);
        when(defaultStreamProvider.get()).thenReturn(Mockito.mock(Stream.class));

        OrderedMessageProcessors orderedMessageProcessors = new OrderedMessageProcessors(
                new HashSet<>(0),
                Mockito.mock(ClusterConfigService.class),
                Mockito.mock(EventBus.class)
        );

        ClusterConfigService clusterConfigService = Mockito.mock(ClusterConfigService.class);
        when(clusterConfigService.get(TimeStampConfig.class)).thenReturn(new TimeStampConfig(gracePeriod));

        return new ProcessBufferProcessor(
                metricRegistry,
                orderedMessageProcessors,
                Mockito.mock(OutputBuffer.class),
                Mockito.mock(ProcessingStatusRecorder.class),
                Mockito.mock(MessageULIDGenerator.class),
                Mockito.mock(DecodingProcessor.class),
                defaultStreamProvider,
                Mockito.mock(FailureSubmissionService.class),
                streamMetrics,
                clusterConfigService
        );
    }
}
