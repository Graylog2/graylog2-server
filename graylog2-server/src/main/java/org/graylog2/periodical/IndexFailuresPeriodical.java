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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import org.graylog2.indexer.IndexFailureService;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class IndexFailuresPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(IndexFailuresPeriodical.class);

    private final IndexFailureService indexFailureService;
    private final Messages messages;
    private final ServerStatus serverStatus;
    private final MetricRegistry metricRegistry;

    @Inject
    public IndexFailuresPeriodical(IndexFailureService indexFailureService,
                                   Messages messages,
                                   ServerStatus serverStatus,
                                   MetricRegistry metricRegistry) {
        this.indexFailureService = indexFailureService;
        this.messages = messages;
        this.serverStatus = serverStatus;
        this.metricRegistry = metricRegistry;
    }

    @Override
    public void initialize() {
        metricRegistry.register(MetricRegistry.name(IndexFailuresPeriodical.class, "queueSize"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return messages.getIndexFailureQueue().size();
            }
        });
    }

    @Override
    public void doRun() {
        while (serverStatus.getLifecycle() != Lifecycle.HALTING) {
            try {
                messages.getIndexFailureQueue()
                        .take()
                        .forEach(indexFailureService::saveWithoutValidation);
            } catch (Exception e) {
                LOG.error("Could not persist index failure.", e);
            }
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public boolean runsForever() {
        return true;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return false;
    }

    @Override
    public boolean parentOnly() {
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
        return 0;
    }
}
