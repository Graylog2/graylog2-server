package org.graylog2.outputs;

import com.beust.jcommander.internal.Lists;
import com.codahale.metrics.MetricRegistry;
import org.graylog2.Configuration;
import org.graylog2.indexer.Indexer;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.outputs.OutputStreamConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class BatchedElasticSearchOutput extends ElasticSearchOutput {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private final List<Message> buffer;
    private final int maxBufferSize;

    @Inject
    public BatchedElasticSearchOutput(MetricRegistry metricRegistry, Indexer indexer, Configuration configuration) {
        super(metricRegistry, indexer);
        this.buffer = Lists.newArrayList();
        this.maxBufferSize = configuration.getOutputBatchSize();
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

    public void flush() throws Exception {
        synchronized (this.buffer) {
            super.write(this.buffer, null);
            this.buffer.clear();
        }
    }
}
