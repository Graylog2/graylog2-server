/*
 * Copyright 2012-2014 TORCH GmbH
 *
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
import org.graylog2.Configuration;
import org.graylog2.indexer.Indexer;
import org.graylog2.plugin.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class BatchedElasticSearchOutput extends ElasticSearchOutput {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private final List<Message> buffer;
    private final int maxBufferSize;
    private final ExecutorService flushThread;
    private final Timer processTime;
    private final Histogram batchSize;
    private final Meter bufferFlushes;
    private final Meter bufferFlushesRequested;

    @Inject
    public BatchedElasticSearchOutput(MetricRegistry metricRegistry, Indexer indexer, Configuration configuration) {
        super(metricRegistry, indexer);
        this.buffer = Lists.newArrayList();
        this.maxBufferSize = configuration.getOutputBatchSize();
        this.flushThread = Executors.newSingleThreadExecutor();
        this.processTime = metricRegistry.timer(name(this.getClass(), "processTime"));
        this.batchSize = metricRegistry.histogram(name(this.getClass(), "batchSize"));
        this.bufferFlushes = metricRegistry.meter(name(this.getClass(), "bufferFlushes"));
        this.bufferFlushesRequested = metricRegistry.meter(name(this.getClass(), "bufferFlushesRequested"));
    }

    @Override
    public void write(Message message) throws Exception {
        synchronized (this.buffer) {
            this.buffer.add(message);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Buffering message id to [{}]: <{}>", getName(), message.getId());
            }
            if (this.buffer.size() >= maxBufferSize) {
                flush();
            }
        }
    }

    @Override
    public String getHumanName() {
        return "ElasticSearch Output with Batching";
    }

    public void synchronousFlush(List<Message> mybuffer) {
        LOG.debug("[{}] Starting flushing {} messages", Thread.currentThread(), mybuffer.size());

        try(Timer.Context context = this.processTime.time()) {
            write(mybuffer);
            this.batchSize.update(mybuffer.size());
            this.bufferFlushes.mark();
        } catch (Exception e) {
            LOG.error("Unable to flush message buffer", e);
        }
        LOG.debug("[{}] Flushing {} messages completed", Thread.currentThread(), mybuffer.size());
    }

    public void asynchronousFlush(final List<Message> mybuffer) {
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

    // used in tests to avoid having to run the executor
    public void flush(boolean async) {
        this.bufferFlushesRequested.mark();
        List<Message> mybuffer;
        synchronized (this.buffer) {
            mybuffer = Lists.newArrayList(this.buffer);
            this.buffer.clear();
        }
        if (async) {
            asynchronousFlush(mybuffer);
        } else {
            synchronousFlush(mybuffer);
        }
    }

}
