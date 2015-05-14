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
package org.graylog2.radio.buffers.processors;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import org.graylog2.plugin.GlobalMetricNames;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.radio.Configuration;
import org.graylog2.radio.transports.RadioTransport;
import org.graylog2.shared.buffers.processors.ProcessBufferProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicInteger;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class RadioProcessBufferProcessor extends ProcessBufferProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(RadioProcessBufferProcessor.class);
    private static final AtomicInteger errorCount = new AtomicInteger(0);
    private final RadioTransport radioTransport;
    private final ServerStatus serverStatus;
    private final int radioTransportMaxErrors;
    private final Meter erroredMessages;
    private final Counter globalOutgoingMessages;

    @Inject
    public RadioProcessBufferProcessor(MetricRegistry metricRegistry,
                                       RadioTransport radioTransport,
                                       ServerStatus serverStatus,
                                       Configuration configuration) {
        super(metricRegistry);
        this.radioTransport = radioTransport;
        this.serverStatus = serverStatus;
        this.radioTransportMaxErrors = configuration.getRadioTransportMaxErrors();
        globalOutgoingMessages = metricRegistry.counter(GlobalMetricNames.OUTPUT_THROUGHPUT);
        this.erroredMessages = metricRegistry.meter(name(RadioProcessBufferProcessor.class, "erroredMessages"));
    }

    @Override
    protected void handleMessage(Message msg) {
        try {
            radioTransport.send(msg);
            globalOutgoingMessages.inc();
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
