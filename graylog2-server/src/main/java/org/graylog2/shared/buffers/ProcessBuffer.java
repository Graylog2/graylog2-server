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
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.graylog2.plugin.GlobalMetricNames;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.buffers.MessageEvent;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.buffers.processors.DecodingProcessor;
import org.graylog2.shared.buffers.processors.ProcessBufferProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.concurrent.ThreadFactory;

import static com.codahale.metrics.MetricRegistry.name;
import static org.graylog2.shared.metrics.MetricUtils.constantGauge;
import static org.graylog2.shared.metrics.MetricUtils.safelyRegister;

@Singleton
public class ProcessBuffer extends Buffer {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessBuffer.class);

    private final Meter incomingMessages;
    private final ProcessBufferProcessor[] processors;

    @Inject
    public ProcessBuffer(MetricRegistry metricRegistry,
                         DecodingProcessor.Factory decodingProcessorFactory,
                         ProcessBufferProcessor.Factory bufferProcessorFactory,
                         @Named("processbuffer_processors") int processorCount,
                         @Named("ring_size") int ringSize,
                         @Named("processor_wait_strategy") String waitStrategyName) {
        this.ringBufferSize = ringSize;
        this.incomingMessages = metricRegistry.meter(name(ProcessBuffer.class, "incomingMessages"));

        final Timer parseTime = metricRegistry.timer(name(ProcessBuffer.class, "parseTime"));
        final Timer decodeTime = metricRegistry.timer(name(ProcessBuffer.class, "decodeTime"));
        safelyRegister(metricRegistry, GlobalMetricNames.PROCESS_BUFFER_USAGE, new Gauge<Long>() {
            @Override
            public Long getValue() {
                return ProcessBuffer.this.getUsage();
            }
        });
        safelyRegister(metricRegistry, GlobalMetricNames.PROCESS_BUFFER_SIZE, constantGauge(ringBufferSize));

        final WaitStrategy waitStrategy = getWaitStrategy(waitStrategyName, "processor_wait_strategy");
        final Disruptor<MessageEvent> disruptor = new Disruptor<>(
                MessageEvent.EVENT_FACTORY,
                ringBufferSize,
                threadFactory(metricRegistry),
                ProducerType.MULTI,
                waitStrategy
        );
        disruptor.setDefaultExceptionHandler(new LoggingExceptionHandler(LOG));

        LOG.info("Initialized ProcessBuffer with ring size <{}> and wait strategy <{}>.",
                ringBufferSize, waitStrategy.getClass().getSimpleName());

        processors = new ProcessBufferProcessor[processorCount];
        for (int i = 0; i < processorCount; i++) {
            processors[i] = bufferProcessorFactory.create(decodingProcessorFactory.create(decodeTime, parseTime));
        }
        disruptor.handleEventsWithWorkerPool(processors);

        ringBuffer = disruptor.start();
    }

    private ThreadFactory threadFactory(MetricRegistry metricRegistry) {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("processbufferprocessor-%d").build();
        return new InstrumentedThreadFactory(
                threadFactory,
                metricRegistry,
                name(this.getClass(), "thread-factory"));
    }

    public void insertBlocking(@Nonnull RawMessage rawMessage) {
        final long sequence = ringBuffer.next();
        final MessageEvent event = ringBuffer.get(sequence);
        event.setRaw(rawMessage);
        ringBuffer.publish(sequence);
        afterInsert(1);
    }

    @Override
    protected void afterInsert(int n) {
        incomingMessages.mark(n);
    }

    public ImmutableMap<String,String> getDump() {
        final ImmutableMap.Builder<String, String> processBufferDump = ImmutableMap.builder();
        for (int i = 0, processorsLength = processors.length; i < processorsLength; i++) {
            final ProcessBufferProcessor proc = processors[i];
            processBufferDump.put("ProcessBufferProcessor #" + i, proc.getCurrentMessage().map(Message::toDumpString).orElse("idle"));
        }
        return processBufferDump.build();
    }
}
