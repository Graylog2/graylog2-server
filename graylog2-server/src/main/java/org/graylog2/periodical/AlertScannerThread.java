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

import org.graylog2.Configuration;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationService;
import org.graylog2.alarmcallbacks.AlarmCallbackFactory;
import org.graylog2.alarmcallbacks.AlarmCallbackHistoryService;
import org.graylog2.alarmcallbacks.EmailAlarmCallback;
import org.graylog2.alerts.AlertScanner;
import org.graylog2.alerts.AlertService;
import org.graylog2.initializers.IndexerSetupService;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

public class AlertScannerThread extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(AlertScannerThread.class);

    private final StreamService streamService;
    private final AlarmCallbackConfigurationService alarmCallbackConfigurationService;
    private final AlarmCallbackFactory alarmCallbackFactory;
    private final EmailAlarmCallback emailAlarmCallback;
    private final IndexerSetupService indexerSetupService;
    private final AlertService alertService;
    private final Configuration configuration;
    private final AlarmCallbackHistoryService alarmCallbackHistoryService;
    private final AlertScanner alertScanner;

    @Inject
    public AlertScannerThread(final AlertService alertService,
                              final StreamService streamService,
                              final AlarmCallbackConfigurationService alarmCallbackConfigurationService,
                              final AlarmCallbackFactory alarmCallbackFactory,
                              final EmailAlarmCallback emailAlarmCallback,
                              final IndexerSetupService indexerSetupService,
                              final Configuration configuration,
                              final AlarmCallbackHistoryService alarmCallbackHistoryService,
                              final AlertScanner alertScanner) {
        this.alertService = alertService;
        this.streamService = streamService;
        this.alarmCallbackConfigurationService = alarmCallbackConfigurationService;
        this.alarmCallbackFactory = alarmCallbackFactory;
        this.emailAlarmCallback = emailAlarmCallback;
        this.indexerSetupService = indexerSetupService;
        this.configuration = configuration;
        this.alarmCallbackHistoryService = alarmCallbackHistoryService;
        this.alertScanner = alertScanner;
    }

    @Override
    public void doRun() {
        if (!indexerSetupService.isRunning()) {
            LOG.error("Indexer is not running, not checking streams for alerts.");
            return;
        }

        LOG.debug("Running alert checks.");
        final List<Stream> alertedStreams = streamService.loadAllWithConfiguredAlertConditions();

        LOG.debug("There are {} streams with configured alert conditions.", alertedStreams.size());

        // Load all streams that have configured alert conditions.
        for (Stream stream : alertedStreams) {
            LOG.debug("Stream [{}] has [{}] configured alert conditions.", stream, streamService.getAlertConditions(stream).size());

            if(stream.isPaused()) {
                LOG.debug("Stream [{}] has been paused. Skipping alert check.", stream);
                continue;
            }

            // Check if a threshold is reached.
            streamService.getAlertConditions(stream).forEach(alertCondition -> alertScanner.checkAlertCondition(stream, alertCondition));
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
    }

    @Override
    public boolean masterOnly() {
        return true;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 10;
    }

    @Override
    public int getPeriodSeconds() {
        return configuration.getAlertCheckInterval();
    }
}
