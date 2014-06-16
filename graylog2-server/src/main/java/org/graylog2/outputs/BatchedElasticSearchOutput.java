package org.graylog2.outputs;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.Lists;
import org.graylog2.Configuration;
import org.graylog2.indexer.Indexer;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.outputs.OutputStreamConfiguration;
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
        this.processTime = metricRegistry.timer(name(BatchedElasticSearchOutput.class, "processTime"));
        this.batchSize = metricRegistry.histogram(name(this.getClass(), "batchSize"));
        this.bufferFlushes = metricRegistry.meter(name(this.getClass(), "bufferFlushes"));
        this.bufferFlushesRequested = metricRegistry.meter(name(this.getClass(), "bufferFlushesRequested"));
    }

    @Override
    public void write(List<Message> messages, OutputStreamConfiguration streamConfig) throws Exception {
        synchronized (this.buffer) {
            this.buffer.addAll(messages);
            if (this.buffer.size() >= maxBufferSize) {
                flush();
            }
        }
    }

    public void synchronousFlush(List<Message> mybuffer) {
        LOG.debug("[{}] Starting flushing {} messages", Thread.currentThread(), mybuffer.size());

        try(Timer.Context context = this.processTime.time()) {
            super.write(mybuffer, null);
            this.batchSize.update(mybuffer.size());
            this.bufferFlushes.mark();
        } catch (Exception e) {
            LOG.error("Unable to flush message buffer: {} - {}", e, e.getStackTrace());
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
        this.bufferFlushesRequested.mark();
        List<Message> mybuffer;
        synchronized (this.buffer) {
            mybuffer = Lists.newArrayList(this.buffer);
            this.buffer.clear();
        }
        asynchronousFlush(mybuffer);
    }
}
