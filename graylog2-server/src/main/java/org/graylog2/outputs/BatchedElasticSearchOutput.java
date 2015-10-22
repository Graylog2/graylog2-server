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
import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.journal.Journal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.codahale.metrics.MetricRegistry.name;

public class BatchedElasticSearchOutput extends ElasticSearchOutput {
    private static final Logger LOG = LoggerFactory.getLogger(BatchedElasticSearchOutput.class);

    private final List<Message> buffer;
    private final int maxBufferSize;
    private final ExecutorService flushThread;
    private final Timer processTime;
    private final Histogram batchSize;
    private final Meter bufferFlushes;
    private final Meter bufferFlushesRequested;
    private final Cluster cluster;

    @AssistedInject
    public BatchedElasticSearchOutput(MetricRegistry metricRegistry,
                                      Messages messages,
                                      Cluster cluster,
                                      org.graylog2.Configuration serverConfiguration,
                                      Journal journal,
                                      @Assisted Stream stream,
                                      @Assisted Configuration configuration) {
        this(metricRegistry, messages, cluster, serverConfiguration, journal);
    }

    @Inject
    public BatchedElasticSearchOutput(MetricRegistry metricRegistry,
                                      Messages messages,
                                      Cluster cluster,
                                      org.graylog2.Configuration serverConfiguration,
                                      Journal journal) {
        super(metricRegistry, messages, journal);
        this.cluster = cluster;
        this.maxBufferSize = serverConfiguration.getOutputBatchSize();
        this.buffer = Lists.newArrayListWithCapacity(maxBufferSize);
        this.processTime = metricRegistry.timer(name(this.getClass(), "processTime"));
        this.batchSize = metricRegistry.histogram(name(this.getClass(), "batchSize"));
        this.bufferFlushes = metricRegistry.meter(name(this.getClass(), "bufferFlushes"));
        this.bufferFlushesRequested = metricRegistry.meter(name(this.getClass(), "bufferFlushesRequested"));
        this.flushThread = executorService(metricRegistry);
    }

    private ExecutorService executorService(final MetricRegistry metricRegistry) {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("batched-elasticsearch-output-%d")
                .build();
        return new InstrumentedExecutorService(
                Executors.newSingleThreadExecutor(threadFactory),
                metricRegistry,
                name(this.getClass(), "executor-service"));
    }

    @Override
    public void write(Message message) throws Exception {
        synchronized (this.buffer) {
            this.buffer.add(message);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Buffering message id to [{}]: <{}>", getClass(), message.getId());
            }
            if (this.buffer.size() >= maxBufferSize) {
                flush();
            }
        }
    }

    private void synchronousFlush(List<Message> messageBuffer) {
        LOG.debug("[{}] Starting flushing {} messages", Thread.currentThread(), messageBuffer.size());

        try (Timer.Context context = this.processTime.time()) {
            write(messageBuffer);
            this.batchSize.update(messageBuffer.size());
            this.bufferFlushes.mark();
        } catch (Exception e) {
            LOG.error("Unable to flush message buffer", e);
        }
        LOG.debug("[{}] Flushing {} messages completed", Thread.currentThread(), messageBuffer.size());
    }

    private void asynchronousFlush(final List<Message> mybuffer) {
        LOG.debug("Submitting new flush thread");
        flushThread.submit(new Runnable() {
            @Override
            public void run() {
                synchronousFlush(mybuffer);
            }
        });
    }

    public void flush() {
        flush(true);
    }

    @VisibleForTesting
    void flush(boolean async) {
        bufferFlushesRequested.mark();

        if (!buffer.isEmpty()) {
            if (cluster.isConnected() && cluster.isHealthy()) {
                final List<Message> temporaryBuffer;
                synchronized (this.buffer) {
                    temporaryBuffer = ImmutableList.copyOf(buffer);
                    buffer.clear();
                }

                if (async) {
                    asynchronousFlush(temporaryBuffer);
                } else {
                    synchronousFlush(temporaryBuffer);
                }
            } else {
                LOG.warn("Clearing buffer ({} messages) because the Elasticsearch cluster is down.", buffer.size());
                buffer.clear();
            }
        } else {
            LOG.debug("Not flushing empty buffer");
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
            super("Batched Elasticsearch Output", false, "", "Elasticsearch Output with Batching");
        }
    }
}
