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
package org.graylog2.shared.buffers;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.InstrumentedThreadFactory;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.GlobalMetricNames;
import org.graylog2.plugin.buffers.InputBuffer;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.concurrent.ThreadFactory;

import static com.codahale.metrics.MetricRegistry.name;
import static org.graylog2.shared.metrics.MetricUtils.constantGauge;
import static org.graylog2.shared.metrics.MetricUtils.safelyRegister;

@Singleton
public class InputBufferImpl implements InputBuffer {
    private static final Logger LOG = LoggerFactory.getLogger(InputBufferImpl.class);

    private final RingBuffer<RawMessageEvent> ringBuffer;
    private final Meter incomingMessages;

    @Inject
    public InputBufferImpl(MetricRegistry metricRegistry,
                           BaseConfiguration configuration,
                           Provider<DirectMessageHandler> directMessageHandlerProvider,
                           Provider<RawMessageEncoderHandler> rawMessageEncoderHandlerProvider,
                           Provider<JournallingMessageHandler> spoolingMessageHandlerProvider) {
        final Disruptor<RawMessageEvent> disruptor = new Disruptor<>(
                RawMessageEvent.FACTORY,
                configuration.getInputBufferRingSize(),
                threadFactory(metricRegistry),
                ProducerType.MULTI,
                configuration.getInputBufferWaitStrategy());
        disruptor.setDefaultExceptionHandler(new LoggingExceptionHandler(LOG));

        final int numberOfHandlers = configuration.getInputbufferProcessors();
        if (configuration.isMessageJournalEnabled()) {
            LOG.info("Message journal is enabled.");

            final RawMessageEncoderHandler[] handlers = new RawMessageEncoderHandler[numberOfHandlers];
            for (int i = 0; i < numberOfHandlers; i++) {
                handlers[i] = rawMessageEncoderHandlerProvider.get();
            }
            disruptor.handleEventsWithWorkerPool(handlers).then(spoolingMessageHandlerProvider.get());
        } else {
            LOG.info("Message journal is disabled.");
            final DirectMessageHandler[] handlers = new DirectMessageHandler[numberOfHandlers];
            for (int i = 0; i < numberOfHandlers; i++) {
                handlers[i] = directMessageHandlerProvider.get();
            }
            disruptor.handleEventsWithWorkerPool(handlers);
        }

        ringBuffer = disruptor.start();

        incomingMessages = metricRegistry.meter(name(InputBufferImpl.class, "incomingMessages"));
        safelyRegister(metricRegistry, GlobalMetricNames.INPUT_BUFFER_USAGE, new Gauge<Long>() {
            @Override
            public Long getValue() {
                return InputBufferImpl.this.getUsage();
            }
        });
        safelyRegister(metricRegistry, GlobalMetricNames.INPUT_BUFFER_SIZE, constantGauge(ringBuffer.getBufferSize()));

        LOG.info("Initialized {} with ring size <{}> and wait strategy <{}>, running {} parallel message handlers.",
                this.getClass().getSimpleName(),
                configuration.getInputBufferRingSize(),
                configuration.getInputBufferWaitStrategy().getClass().getSimpleName(),
                numberOfHandlers);
    }

    @Override
    public void insert(RawMessage message) {
        ringBuffer.publishEvent(RawMessageEvent.TRANSLATOR, message);
        incomingMessages.mark();
    }

    @Override
    public long getUsage() {
        return ringBuffer.getBufferSize() - ringBuffer.remainingCapacity();
    }

    private ThreadFactory threadFactory(final MetricRegistry metricRegistry) {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("inputbufferprocessor-%d").build();
        return new InstrumentedThreadFactory(threadFactory, metricRegistry, name(this.getClass(), "thread-factory"));
    }

}
