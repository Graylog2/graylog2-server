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
package org.graylog2.outputs;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.indexer.IndexSet;
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
import java.util.Map;
import java.util.concurrent.TimeoutException;
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

    private volatile List<Map.Entry<IndexSet, Message>> buffer;

    private static final AtomicInteger activeFlushThreads = new AtomicInteger(0);
    private final AtomicLong lastFlushTime = new AtomicLong();
    private final int outputFlushInterval;

    @AssistedInject
    public BlockingBatchedESOutput(MetricRegistry metricRegistry,
                                   Messages messages,
                                   Cluster cluster,
                                   org.graylog2.Configuration serverConfiguration,
                                   Journal journal,
                                   @Assisted Stream stream,
                                   @Assisted Configuration configuration) {
        this(metricRegistry, messages, cluster, serverConfiguration, journal);
    }

    @Inject
    public BlockingBatchedESOutput(MetricRegistry metricRegistry,
                                   Messages messages,
                                   Cluster cluster,
                                   org.graylog2.Configuration serverConfiguration,
                                   Journal journal) {
        super(metricRegistry, messages, journal);
        this.cluster = cluster;
        this.maxBufferSize = serverConfiguration.getOutputBatchSize();
        outputFlushInterval = serverConfiguration.getOutputFlushInterval();
        this.processTime = metricRegistry.timer(name(this.getClass(), "processTime"));
        this.batchSize = metricRegistry.histogram(name(this.getClass(), "batchSize"));
        this.bufferFlushes = metricRegistry.meter(name(this.getClass(), "bufferFlushes"));
        this.bufferFlushesRequested = metricRegistry.meter(name(this.getClass(), "bufferFlushesRequested"));

        buffer = Lists.newArrayListWithCapacity(maxBufferSize);

    }

    @Override
    public void write(Message message) throws Exception {
        for (IndexSet indexSet : message.getIndexSets()) {
            writeMessageEntry(Maps.immutableEntry(indexSet, message));
        }
    }

    public void writeMessageEntry(Map.Entry<IndexSet, Message> entry) throws Exception {
        List<Map.Entry<IndexSet, Message>> flushBatch = null;
        synchronized (this) {
            buffer.add(entry);

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

    private void flush(List<Map.Entry<IndexSet, Message>> messages) {
        if (!cluster.isConnected() || !cluster.isDeflectorHealthy()) {
            try {
                cluster.waitForConnectedAndDeflectorHealthy();
            } catch (TimeoutException | InterruptedException e) {
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
            writeMessageEntries(messages);
            batchSize.update(messages.size());
            bufferFlushes.mark();
        } catch (Exception e) {
            log.error("Unable to flush message buffer", e);
        }
        activeFlushThreads.decrementAndGet();
        log.debug("Flushing {} messages completed", messages.size());
    }

    public void forceFlushIfTimedout() {
        if (!cluster.isConnected() || !cluster.isDeflectorHealthy()) {
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
        final List<Map.Entry<IndexSet, Message>> flushBatch;
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
    }

    public static class Config extends ElasticSearchOutput.Config {
    }

    public static class Descriptor extends ElasticSearchOutput.Descriptor {
        public Descriptor() {
            super("Blocking Batched Elasticsearch Output", false, "", "Elasticsearch Output with Batching (blocking)");
        }
    }
}
