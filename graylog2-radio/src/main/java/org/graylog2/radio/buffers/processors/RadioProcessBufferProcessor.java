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
package org.graylog2.radio.buffers.processors;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.radio.Configuration;
import org.graylog2.radio.transports.RadioTransport;
import org.graylog2.shared.buffers.processors.ProcessBufferProcessor;
import org.graylog2.shared.stats.ThroughputStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class RadioProcessBufferProcessor extends ProcessBufferProcessor {
    public interface Factory {
        public RadioProcessBufferProcessor create(
                @Assisted("ordinal") final long ordinal,
                @Assisted("numberOfConsumers") final long numberOfConsumers
        );
    }

    private static final Logger LOG = LoggerFactory.getLogger(RadioProcessBufferProcessor.class);
    private static final AtomicInteger errorCount = new AtomicInteger(0);
    private final ThroughputStats throughputStats;
    private final RadioTransport radioTransport;
    private final ServerStatus serverStatus;
    private final int radioTransportMaxErrors;
    private final Meter erroredMessages;

    @AssistedInject
    public RadioProcessBufferProcessor(MetricRegistry metricRegistry,
                                       ThroughputStats throughputStats,
                                       RadioTransport radioTransport,
                                       ServerStatus serverStatus,
                                       Configuration configuration,
                                       @Assisted("ordinal") final long ordinal,
                                       @Assisted("numberOfConsumers") final long numberOfConsumers) {
        super(metricRegistry, ordinal, numberOfConsumers);
        this.throughputStats = throughputStats;
        this.radioTransport = radioTransport;
        this.serverStatus = serverStatus;
        this.radioTransportMaxErrors = configuration.getRadioTransportMaxErrors();
        this.erroredMessages = metricRegistry.meter(name(RadioProcessBufferProcessor.class, "erroredMessages"));
    }

    @Override
    protected void handleMessage(Message msg) {
        try {
            radioTransport.send(msg);
            throughputStats.getThroughputCounter().add(1);
            errorCount.set(0);
            if (LOG.isDebugEnabled())
                LOG.debug("Message <{}> written to RadioTransport.", msg.getId());
        } catch (Exception e) {
            int errors = errorCount.addAndGet(1);
            if (radioTransportMaxErrors > 0 && errors >= radioTransportMaxErrors) {
                serverStatus.pauseMessageProcessing();
                serverStatus.overrideLoadBalancerDead();
                LOG.error("Number of Radio transport errors exceeded threshold ({}), switching to lb:dead.", radioTransportMaxErrors);
            }
            erroredMessages.mark();
            LOG.error("[Error #{}] Caught exception while sending message to Radio transport: ", errors, e);
        }
    }
}
