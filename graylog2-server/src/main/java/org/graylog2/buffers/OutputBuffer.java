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
package org.graylog2.buffers;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.graylog2.Configuration;
import org.graylog2.buffers.processors.OutputBufferProcessor;
import org.graylog2.inputs.Cache;
import org.graylog2.inputs.OutputCache;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;
import org.graylog2.plugin.buffers.MessageEvent;
import org.graylog2.plugin.buffers.ProcessingDisabledException;
import org.graylog2.plugin.inputs.MessageInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
*/
@Singleton
public class OutputBuffer extends Buffer {
    private static final Logger LOG = LoggerFactory.getLogger(OutputBuffer.class);

    protected ExecutorService executor = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder()
                .setNameFormat("outputbufferprocessor-%d")
                .build()
    );

    private final OutputBufferWatermark outputBufferWatermark;

    private final Configuration configuration;
    private final OutputCache overflowCache;

    private final Meter incomingMessages;
    private final Meter rejectedMessages;
    private final Meter cachedMessages;
    private final Timer processTime;

    private final OutputBufferProcessor.Factory outputBufferProcessorFactory;

    @Inject
    public OutputBuffer(OutputBufferProcessor.Factory outputBufferProcessorFactory,
                        MetricRegistry metricRegistry,
                        OutputBufferWatermark outputBufferWatermark,
                        Configuration configuration,
                        OutputCache overflowCache) {
        this.outputBufferProcessorFactory = outputBufferProcessorFactory;
        this.outputBufferWatermark = outputBufferWatermark;
        this.configuration = configuration;
        this.overflowCache = overflowCache;

        incomingMessages = metricRegistry.meter(name(OutputBuffer.class, "incomingMessages"));
        rejectedMessages = metricRegistry.meter(name(OutputBuffer.class, "rejectedMessages"));
        cachedMessages = metricRegistry.meter(name(OutputBuffer.class, "cachedMessages"));
        processTime = metricRegistry.timer(name(OutputBuffer.class, "processTime"));
    }

    public Cache getOverflowCache() {
        return overflowCache;
    }

    public void initialize() {
        Disruptor disruptor = new Disruptor<MessageEvent>(
                MessageEvent.EVENT_FACTORY,
                configuration.getRingSize(),
                executor,
                ProducerType.MULTI,
                configuration.getProcessorWaitStrategy()
        );
        
        LOG.info("Initialized OutputBuffer with ring size <{}> "
                + "and wait strategy <{}>.", configuration.getRingSize(),
                configuration.getProcessorWaitStrategy().getClass().getSimpleName());

        int outputBufferProcessorCount = configuration.getOutputBufferProcessors();

        OutputBufferProcessor[] processors = new OutputBufferProcessor[outputBufferProcessorCount];
        
        for (int i = 0; i < outputBufferProcessorCount; i++) {
            processors[i] = outputBufferProcessorFactory.create(i, outputBufferProcessorCount);
        }
        
        disruptor.handleEventsWith(processors);
        
        ringBuffer = disruptor.start();
    }

    @Override
    public void insertCached(Message message, MessageInput sourceInput) {
        if (!hasCapacity()) {
            LOG.debug("Out of capacity. Writing to cache.");
            cachedMessages.mark();
            overflowCache.add(message);
            return;
        }

        insert(message);
        afterInsert(1);
    }

    @Override
    public void insertFailFast(Message message, MessageInput sourceInput) throws BufferOutOfCapacityException {
        if (!hasCapacity()) {
            LOG.debug("Rejecting message, because I am full and caching was disabled by input. Raise my size or add more processors.");
            rejectedMessages.mark();
            throw new BufferOutOfCapacityException();
        }
        
        insertBlocking(message, sourceInput);
    }

    @Override
    public void insertCached(List<Message> messages) {
        int length = messages.size();
        if (!hasCapacity(length)) {
            LOG.debug("Out of capacity. Writing to cache.");
            cachedMessages.mark(length);
            overflowCache.add(messages);
            return;
        }

        insert(messages.toArray(new Message[length]));
        afterInsert(length);
    }

    @Override
    public void insertFailFast(List<Message> messages) throws BufferOutOfCapacityException {
        int length = messages.size();
        if (!hasCapacity(length)) {
            LOG.debug("Rejecting message, because I am full and caching was disabled by input. Raise my size or add more processors.");
            rejectedMessages.mark(length);
            throw new BufferOutOfCapacityException();
        }

        insertBlocking(messages);
    }

    @Override
    public void insertBlocking(Message message, MessageInput messageInput) {
        final Timer.Context timer = this.processTime.time();
        insert(message);
        afterInsert(1);
        final long time = timer.stop();
        LOG.debug("It took me {}ns to put the message on the ring.", time);
    }

    @Override
    public void insertBlocking(List<Message> messages) {
        final Timer.Context timer = this.processTime.time();
        int length = messages.size();

        insert(messages.toArray(new Message[length]));
        afterInsert(length);
        final long time = timer.stop();
        LOG.debug("It took me {}ns to put {} messages on the ring.", time, length);
    }

    @Override
    protected void afterInsert(int n) {
        outputBufferWatermark.addAndGet(n);
        incomingMessages.mark(n);
    }
}
