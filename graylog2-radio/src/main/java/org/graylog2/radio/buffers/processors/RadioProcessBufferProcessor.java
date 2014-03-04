package org.graylog2.radio.buffers.processors;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.Message;
import org.graylog2.radio.transports.RadioTransport;
import org.graylog2.shared.buffers.processors.ProcessBufferProcessor;
import org.graylog2.shared.stats.ThroughputStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class RadioProcessBufferProcessor extends ProcessBufferProcessor {
    public interface Factory {
        public RadioProcessBufferProcessor create(
                GraylogServer server,
                @Assisted("ordinal") final long ordinal,
                @Assisted("numberOfConsumers") final long numberOfConsumers,
                RadioTransport radioTransport
        );
    }

    private static final Logger LOG = LoggerFactory.getLogger(RadioProcessBufferProcessor.class);
    private final ThroughputStats throughputStats;
    private final RadioTransport radioTransport;

    @AssistedInject
    public RadioProcessBufferProcessor(MetricRegistry metricRegistry,
                                       ThroughputStats throughputStats,
                                       @Assisted GraylogServer server,
                                       @Assisted("ordinal") final long ordinal,
                                       @Assisted("numberOfConsumers") final long numberOfConsumers,
                                       @Assisted RadioTransport radioTransport) {
        super(metricRegistry, server.processBufferWatermark(), ordinal, numberOfConsumers);
        this.throughputStats = throughputStats;
        this.radioTransport = radioTransport;
    }

    @Override
    protected void handleMessage(Message msg) {
        radioTransport.send(msg);
        throughputStats.getThroughputCounter().add(1);
        LOG.debug("Message <{}> written to RadioTransport.", msg.getId());
    }
}
