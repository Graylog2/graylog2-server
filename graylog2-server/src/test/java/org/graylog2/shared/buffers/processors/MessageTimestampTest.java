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
import org.graylog2.cluster.ClusterConfigChangedEvent;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageTimestampTest {
    static final int GRACE_PERIOD_DAYS = 10;
    final MessageFactory messageFactory = new TestMessageFactory();
    ClusterConfigService clusterConfigService;
    ProcessBufferProcessor processBufferProcessor;
    DateTime initialTime;

    @BeforeEach
    void setUp() {
        initialTime = new DateTime(2014, 1, 1, 1, 59, 59, 0, DateTimeZone.UTC);
        final InstantMillisProvider clock = new InstantMillisProvider(initialTime);
        DateTimeUtils.setCurrentMillisProvider(clock);

        processBufferProcessor = createProcessor(Duration.ofDays(GRACE_PERIOD_DAYS));
    }

    @AfterEach
    void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
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

    @Test
    void testEventHandler() {
        Duration currentGracePeriod = processBufferProcessor.getTimeStampGracePeriod();
        Duration newGracePeriod = currentGracePeriod.plusDays(1);
        setClusterConfigValue(newGracePeriod);

        processBufferProcessor.handleGracePeriodUpdated(getClusterConfigChangedEvent());

        assertThat(processBufferProcessor.getTimeStampGracePeriod()).isEqualTo(newGracePeriod);
    }

    @Test
    void testNullGracePeriodIsCached() {
        final ProcessBufferProcessor processor = createProcessor(null);

        // First call loads from cluster config
        assertThat(processor.getTimeStampGracePeriod()).isNull();
        // Second call should use cache, not hit cluster config again
        assertThat(processor.getTimeStampGracePeriod()).isNull();

        // getOrDefault should have been called exactly once, proving the cache works for null
        verify(clusterConfigService, Mockito.times(1))
                .getOrDefault(eq(TimeStampConfig.class), Mockito.any());
    }

    @Test
    void testNullGracePeriodCacheInvalidatesOnEvent() {
        final ProcessBufferProcessor processor = createProcessor(null);

        // Load and cache null value
        assertThat(processor.getTimeStampGracePeriod()).isNull();

        // Update config to a real value and invalidate cache
        final Duration newGracePeriod = Duration.ofDays(5);
        setClusterConfigValue(newGracePeriod);
        processor.handleGracePeriodUpdated(getClusterConfigChangedEvent());

        // Should read from cluster config again and get the new value
        assertThat(processor.getTimeStampGracePeriod()).isEqualTo(newGracePeriod);
    }

    @Test
    void testNonNullGracePeriodIsCached() {
        // First call loads from cluster config
        assertThat(processBufferProcessor.getTimeStampGracePeriod()).isEqualTo(Duration.ofDays(GRACE_PERIOD_DAYS));
        // Second call should use cache
        assertThat(processBufferProcessor.getTimeStampGracePeriod()).isEqualTo(Duration.ofDays(GRACE_PERIOD_DAYS));

        verify(clusterConfigService, Mockito.times(1))
                .getOrDefault(eq(TimeStampConfig.class), Mockito.any());
    }

    @Test
    void testTransitionFromEnabledToDisabled() {
        // Start with normalization enabled
        final ProcessBufferProcessor processor = createProcessor(Duration.ofDays(2));
        assertThat(processor.getTimeStampGracePeriod()).isEqualTo(Duration.ofDays(2));

        // User disables normalization — config changes to null grace period
        setClusterConfigValue(null);
        processor.handleGracePeriodUpdated(getClusterConfigChangedEvent());

        // Should read null and cache it (not query again)
        assertThat(processor.getTimeStampGracePeriod()).isNull();
        assertThat(processor.getTimeStampGracePeriod()).isNull();

        // Two getOrDefault calls total: one for initial load, one after invalidation
        verify(clusterConfigService, Mockito.times(2))
                .getOrDefault(eq(TimeStampConfig.class), Mockito.any());
    }

    @Test
    void testTransitionFromDisabledToEnabled() {
        // Start with normalization disabled (null grace period)
        final ProcessBufferProcessor processor = createProcessor(null);
        assertThat(processor.getTimeStampGracePeriod()).isNull();

        // User enables normalization
        final Duration newGracePeriod = Duration.ofDays(2);
        setClusterConfigValue(newGracePeriod);
        processor.handleGracePeriodUpdated(getClusterConfigChangedEvent());

        assertThat(processor.getTimeStampGracePeriod()).isEqualTo(newGracePeriod);
        assertThat(processor.getTimeStampGracePeriod()).isEqualTo(newGracePeriod);

        // Two getOrDefault calls total: one for initial load, one after invalidation
        verify(clusterConfigService, Mockito.times(2))
                .getOrDefault(eq(TimeStampConfig.class), Mockito.any());
    }

    @Test
    void testExistingNullDocumentInMongoDB() {
        // Simulates backwards-compatible case: install already has a TimeStampConfig
        // document with null grace_period from a previous save
        final ProcessBufferProcessor processor = createProcessor(null);

        // Multiple messages processed — should only query once
        for (int i = 0; i < 100; i++) {
            assertThat(processor.getTimeStampGracePeriod()).isNull();
        }

        verify(clusterConfigService, Mockito.times(1))
                .getOrDefault(eq(TimeStampConfig.class), Mockito.any());
    }

    @Test
    void testDefaultConfigWhenNoDocumentExists() {
        // Simulates fresh install: no TimeStampConfig document in MongoDB
        // getOrDefault returns TimeStampConfig.getDefault() which has a distant-future grace period
        clusterConfigService = Mockito.mock(ClusterConfigService.class);
        when(clusterConfigService.getOrDefault(eq(TimeStampConfig.class), Mockito.any()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        final ProcessBufferProcessor processor = createProcessorWithConfigService(clusterConfigService);
        final Duration expectedDefault = TimeStampConfig.getDefault().gracePeriod();

        // Multiple messages processed — should only query once
        for (int i = 0; i < 100; i++) {
            assertThat(processor.getTimeStampGracePeriod()).isEqualTo(expectedDefault);
        }

        verify(clusterConfigService, Mockito.times(1))
                .getOrDefault(eq(TimeStampConfig.class), Mockito.any());
    }

    private void setClusterConfigValue(Duration gracePeriod) {
        when(clusterConfigService.get(TimeStampConfig.class)).thenReturn(new TimeStampConfig(gracePeriod));
        when(clusterConfigService.getOrDefault(eq(TimeStampConfig.class), Mockito.any())).thenReturn(new TimeStampConfig(gracePeriod));
    }

    private ClusterConfigChangedEvent getClusterConfigChangedEvent() {
        return ClusterConfigChangedEvent.create(DateTime.now(DateTimeZone.UTC), "node-id", TimeStampConfig.class.getName());
    }

    ProcessBufferProcessor createProcessor(Duration gracePeriod) {
        clusterConfigService = Mockito.mock(ClusterConfigService.class);
        setClusterConfigValue(gracePeriod);
        return createProcessorWithConfigService(clusterConfigService);
    }

    ProcessBufferProcessor createProcessorWithConfigService(ClusterConfigService configService) {
        final MetricRegistry metricRegistry = new MetricRegistry();
        final StreamMetrics streamMetrics = new StreamMetrics(metricRegistry);

        final Provider<Stream> defaultStreamProvider = Mockito.mock(Provider.class);
        when(defaultStreamProvider.get()).thenReturn(Mockito.mock(Stream.class));

        final OrderedMessageProcessors orderedMessageProcessors = new OrderedMessageProcessors(
                new HashSet<>(0),
                Mockito.mock(ClusterConfigService.class),
                Mockito.mock(EventBus.class)
        );

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
                configService,
                Mockito.mock(EventBus.class)
        );
    }
}
