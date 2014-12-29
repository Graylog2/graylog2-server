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
package org.graylog2.outputs;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.Lists;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.journal.Journal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.codahale.metrics.MetricRegistry.name;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

// Singleton class
public class BlockingBatchedESOutput extends ElasticSearchOutput {
    private static final Logger log = LoggerFactory.getLogger(BlockingBatchedESOutput.class);
    private final Cluster cluster;
    private final int maxBufferSize;
    private final Timer processTime;
    private final Histogram batchSize;
    private final Meter bufferFlushes;
    private final Meter bufferFlushesRequested;

    private volatile List<Message> buffer;

    private ScheduledExecutorService daemonExecutor;
    private int processorCount;
    private static final AtomicInteger activeFlushThreads = new AtomicInteger(0);
    private final AtomicLong lastFlushTime = new AtomicLong();
    private int outputFlushInterval;

    @AssistedInject
    public BlockingBatchedESOutput(MetricRegistry metricRegistry,
                                   Messages messages,
                                   Cluster cluster,
                                   org.graylog2.Configuration serverConfiguration,
                                   Journal journal,
                                   @Named("daemonScheduler") ScheduledExecutorService daemonExecutor,
                                   @Assisted Stream stream,
                                   @Assisted Configuration configuration) {
        this(metricRegistry, messages, cluster, daemonExecutor, serverConfiguration, journal);
    }

    @Inject
    public BlockingBatchedESOutput(MetricRegistry metricRegistry,
                                   Messages messages,
                                   Cluster cluster,
                                   @Named("daemonScheduler") ScheduledExecutorService daemonExecutor,
                                   org.graylog2.Configuration serverConfiguration,
                                   Journal journal) {
        super(metricRegistry, messages, journal);
        this.cluster = cluster;
        this.daemonExecutor = daemonExecutor;
        this.maxBufferSize = serverConfiguration.getOutputBatchSize();
        processorCount = serverConfiguration.getOutputBufferProcessors();
        outputFlushInterval = serverConfiguration.getOutputFlushInterval();
        this.processTime = metricRegistry.timer(name(this.getClass(), "processTime"));
        this.batchSize = metricRegistry.histogram(name(this.getClass(), "batchSize"));
        this.bufferFlushes = metricRegistry.meter(name(this.getClass(), "bufferFlushes"));
        this.bufferFlushesRequested = metricRegistry.meter(name(this.getClass(), "bufferFlushesRequested"));

        buffer = Lists.newArrayListWithCapacity(maxBufferSize);

    }

    @Override
    public void write(Message message) throws Exception {
        List<Message> flushBatch = null;
        synchronized (this) {
            buffer.add(message);

            if (buffer.size() >= maxBufferSize) {
                flushBatch = buffer;
                buffer = Lists.newArrayListWithCapacity(maxBufferSize);
            }
        }
        // if the current thread found it had to flush any messages, it does so but blocks.
        // this ensures we don't flush more than 'processorCount' in parallel.
        // TODO this will still be time limited by the OutputBufferProcessor and thus be called more often than it should
        if (flushBatch != null) {
            flush(flushBatch);
        }
    }

    private void flush(List<Message> messages) {
        if (!cluster.isConnectedAndHealthy()) {
            try {
                cluster.waitForConnectedAndHealthy();
            } catch (InterruptedException e) {
                log.warn("Error while waiting for healthy Elasticsearch cluster. Not flushing.", e);
                return;
            }
        }
        // never try to flush an empty buffer
        if (messages.size() == 0) {
            return;
        }
        log.debug("Starting flushing {} messages, flush threads active {}",
                 messages.size(),
                 activeFlushThreads.incrementAndGet());

        try (Timer.Context ignored = processTime.time()) {
            lastFlushTime.set(System.nanoTime());
            write(messages);
            batchSize.update(messages.size());
            bufferFlushes.mark();
        } catch (Exception e) {
            log.error("Unable to flush message buffer", e);
        }
        activeFlushThreads.decrementAndGet();
        log.debug("Flushing {} messages completed", messages.size());
    }

    public void forceFlushIfTimedout() {
        if (!cluster.isConnectedAndHealthy()) {
            // do not actually try to flush, because that will block until the cluster comes back.
            // simply check and return.
            log.debug("Cluster unavailable, but not blocking for periodic flush attempt. This will try again.");
            return;
        }
        // if we shouldn't flush at all based on the last flush time, no need to synchronize on this.
        if (lastFlushTime.get() != 0 &&
                outputFlushInterval > NANOSECONDS.toSeconds(System.nanoTime() - lastFlushTime.get())) {
                    return;
                }
        // flip buffer quickly and initiate flush
        final List<Message> flushBatch;
        synchronized (this) {
            flushBatch = buffer;
            buffer = Lists.newArrayListWithCapacity(maxBufferSize);
        }
        if (flushBatch != null) {
            bufferFlushesRequested.mark();
            flush(flushBatch);
        }
    }

    public interface Factory extends ElasticSearchOutput.Factory {
        @Override
        BatchedElasticSearchOutput create(Stream stream, Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Config extends ElasticSearchOutput.Config {
    }

    public static class Descriptor extends ElasticSearchOutput.Descriptor {
        public Descriptor() {
            super("Blocking Batched Elasticsearch Output", false, "", "Elasticsearch Output with Batching (blocking)");
        }
    }
}
