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
package org.graylog2.buffers;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.InstrumentedThreadFactory;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.graylog2.buffers.processors.OutputBufferProcessor;
import org.graylog2.plugin.GlobalMetricNames;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.buffers.MessageEvent;
import org.graylog2.shared.buffers.LoggingExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.concurrent.ThreadFactory;

import static com.codahale.metrics.MetricRegistry.name;
import static org.graylog2.shared.metrics.MetricUtils.constantGauge;
import static org.graylog2.shared.metrics.MetricUtils.safelyRegister;

@Singleton
public class OutputBuffer extends Buffer {
    private static final Logger LOG = LoggerFactory.getLogger(OutputBuffer.class);

    private final Meter incomingMessages;

    @Inject
    public OutputBuffer(MetricRegistry metricRegistry,
                        Provider<OutputBufferProcessor> processorProvider,
                        @Named("outputbuffer_processors") int processorCount,
                        @Named("ring_size") int ringSize,
                        @Named("processor_wait_strategy") String waitStrategyName) {
        this.ringBufferSize = ringSize;
        this.incomingMessages = metricRegistry.meter(name(OutputBuffer.class, "incomingMessages"));

        safelyRegister(metricRegistry, GlobalMetricNames.OUTPUT_BUFFER_USAGE, new Gauge<Long>() {
            @Override
            public Long getValue() {
                return OutputBuffer.this.getUsage();
            }
        });
        safelyRegister(metricRegistry, GlobalMetricNames.OUTPUT_BUFFER_SIZE, constantGauge(ringBufferSize));

        final ThreadFactory threadFactory = threadFactory(metricRegistry);
        final WaitStrategy waitStrategy = getWaitStrategy(waitStrategyName, "processor_wait_strategy");
        final Disruptor<MessageEvent> disruptor = new Disruptor<>(
                MessageEvent.EVENT_FACTORY,
                this.ringBufferSize,
                threadFactory,
                ProducerType.MULTI,
                waitStrategy
        );
        disruptor.setDefaultExceptionHandler(new LoggingExceptionHandler(LOG));

        LOG.info("Initialized OutputBuffer with ring size <{}> and wait strategy <{}>.",
                ringBufferSize, waitStrategy.getClass().getSimpleName());

        final OutputBufferProcessor[] processors = new OutputBufferProcessor[processorCount];

        for (int i = 0; i < processorCount; i++) {
            processors[i] = processorProvider.get();
        }

        disruptor.handleEventsWithWorkerPool(processors);

        ringBuffer = disruptor.start();
    }

    private ThreadFactory threadFactory(final MetricRegistry metricRegistry) {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("outputbufferprocessor-%d").build();
        return new InstrumentedThreadFactory(threadFactory, metricRegistry, name(this.getClass(), "thread-factory"));
    }

    public void insertBlocking(Message message) {
        insert(message);
    }

    @Override
    protected void afterInsert(int n) {
        incomingMessages.mark(n);
    }
}
