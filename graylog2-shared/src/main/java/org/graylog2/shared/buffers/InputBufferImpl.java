/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.shared.buffers;

import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.buffers.InputBuffer;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.codahale.metrics.MetricRegistry.name;


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
                executorService(metricRegistry),
                ProducerType.MULTI,
                configuration.getInputBufferWaitStrategy());
        disruptor.handleExceptionsWith(new LoggingExceptionHandler(LOG));

        final int numberOfHandlers = configuration.getInputbufferProcessors();
        if (configuration.isMessageJournalEnabled()) {
            LOG.info("Message journal is enabled.");

            final RawMessageEncoderHandler[] handlers = new RawMessageEncoderHandler[numberOfHandlers];
            for (int i = 0 ; i < numberOfHandlers; i++) {
                handlers[i] = rawMessageEncoderHandlerProvider.get();
            }
            disruptor.handleEventsWithWorkerPool(handlers)
                    .then(spoolingMessageHandlerProvider.get());
        } else{
            LOG.info("Message journal is disabled.");
            final DirectMessageHandler[] handlers = new DirectMessageHandler[numberOfHandlers];
            for (int i = 0; i < numberOfHandlers; i++) {
                handlers[i] = directMessageHandlerProvider.get();
            }
            disruptor.handleEventsWithWorkerPool(handlers);
        }

        ringBuffer = disruptor.start();

        incomingMessages = metricRegistry.meter(name(InputBufferImpl.class, "incomingMessages"));

        LOG.info("Initialized {} with ring size <{}> and wait strategy <{}>, running {} parallel message handlers.",
                this.getClass().getSimpleName(),
                configuration.getInputBufferRingSize(),
                configuration.getInputBufferWaitStrategy().getClass().getSimpleName(),
                numberOfHandlers);
    }

    public void insert(RawMessage message) {
        ringBuffer.publishEvent(RawMessageEvent.TRANSLATOR, message);
        incomingMessages.mark();
    }

    @Override
    public long size() {
        return ringBuffer.getBufferSize() - ringBuffer.remainingCapacity();
    }

    private ExecutorService executorService(final MetricRegistry metricRegistry) {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("inputbufferprocessor-%d").build();
        return new InstrumentedExecutorService(Executors.newCachedThreadPool(threadFactory), metricRegistry, name(this.getClass(), "executor-service"));
    }

}
