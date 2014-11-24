/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.shared.buffers;

import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.InstrumentedThreadFactory;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.buffers.InputBuffer;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.codahale.metrics.MetricRegistry.name;


@Singleton
public class InputBufferImpl implements InputBuffer {
    private static final Logger LOG = LoggerFactory.getLogger(InputBufferImpl.class);

    private final RingBuffer<RawMessageEvent> ringBuffer;

    @Inject
    public InputBufferImpl(MetricRegistry metricRegistry,
                           BaseConfiguration configuration,
                           Provider<DirectMessageHandler> directMessageHandlerProvider,
                           Provider<RawMessageEncoderHandler> rawMessageEncoderHandlerProvider,
                           Provider<JournallingMessageHandler> spoolingMessageHandlerProvider) {
        final Disruptor<RawMessageEvent> disruptor = new Disruptor<>(
                RawMessageEvent.FACTORY,
                configuration.getInputBufferRingSize(),
                executorService(metricRegistry),
                ProducerType.MULTI,
                configuration.getInputBufferWaitStrategy());

        disruptor.handleExceptionsWith(new ExceptionHandler() {
            @Override
            public void handleEventException(Throwable ex, long sequence, Object event) {
                LOG.error("", ex);
            }

            @Override
            public void handleOnStartException(Throwable ex) {
                LOG.error("", ex);
            }

            @Override
            public void handleOnShutdownException(Throwable ex) {
                LOG.error("", ex);
            }
        });
        if (configuration.isMessageJournalEnabled()) {
            LOG.info("Message journal is enabled.");
            disruptor.handleEventsWithWorkerPool(rawMessageEncoderHandlerProvider.get()) // TODO count based on config
                    .then(spoolingMessageHandlerProvider.get());
        } else{
            LOG.info("Message journal is disabled.");
            disruptor.handleEventsWithWorkerPool(directMessageHandlerProvider.get()); // TODO count based on config
        }

        ringBuffer = disruptor.start();

        LOG.info("Initialized {} with ring size <{}> and wait strategy <{}>.",
                this.getClass().getSimpleName(),
                configuration.getInputBufferRingSize(),
                configuration.getInputBufferWaitStrategy().getClass().getSimpleName());
    }

    public void insert(RawMessage message) {
        ringBuffer.publishEvent(RawMessageEvent.TRANSLATOR, message);
    }

    private ExecutorService executorService(final MetricRegistry metricRegistry) {
        return new InstrumentedExecutorService(Executors.newCachedThreadPool(
                threadFactory(metricRegistry)), metricRegistry, name(this.getClass(), "executor-service"));
    }

    private ThreadFactory threadFactory(MetricRegistry metricRegistry) {
        return new InstrumentedThreadFactory(
                new ThreadFactoryBuilder().setNameFormat("inputbufferprocessor-%d").build(),
                metricRegistry);
    }

}
