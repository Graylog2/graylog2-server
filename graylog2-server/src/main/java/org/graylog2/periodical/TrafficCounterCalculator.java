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
package org.graylog2.periodical;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.github.joschi.jadconfig.util.Size;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.system.traffic.TrafficCounterService;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class TrafficCounterCalculator extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(TrafficCounterCalculator.class);
    private final NodeId nodeId;
    private final TrafficCounterService trafficService;
    private final MetricRegistry metricRegistry;
    private long previousOutputBytes = 0L;
    private Counter outputCounter;

    @Inject
    public TrafficCounterCalculator(NodeId nodeId, TrafficCounterService trafficService, MetricRegistry metricRegistry) {
        this.nodeId = nodeId;
        this.trafficService = trafficService;
        this.metricRegistry = metricRegistry;
    }

    @Override
    public void initialize() {
        outputCounter = metricRegistry.counter(Messages.OUTPUT_BYTES_COUNTER_NAME);
    }

    @Override
    public void doRun() {
        final DateTime now = Tools.nowUTC();
        final int secondOfMinute = now.getSecondOfMinute();
        // on the top of every minute, we flush the current throughput
        if (secondOfMinute == 0) {
            LOG.warn("Calculating previous minutes' output traffic");
            final long currentOutputBytes = outputCounter.getCount();
            final long bytesLastMinute = currentOutputBytes - previousOutputBytes;
            previousOutputBytes = currentOutputBytes;
            if (LOG.isWarnEnabled()) {
                final Size bytes = Size.bytes(bytesLastMinute);
                LOG.warn("Elasticsearch output wrote {} ({} MB) during the previous minute.", bytes, bytes.toMegabytes());
            }
            final DateTime previousMinute = now.minusMinutes(1);
            trafficService.updateOutputTraffic(previousMinute, nodeId, bytesLastMinute);
        }
    }


    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
    }

    @Override
    public boolean masterOnly() {
        return false;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 1;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
