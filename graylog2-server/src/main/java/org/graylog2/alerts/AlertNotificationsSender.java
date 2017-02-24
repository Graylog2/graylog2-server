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
package org.graylog2.alerts;

import org.graylog2.alarmcallbacks.AlarmCallbackConfiguration;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationService;
import org.graylog2.alarmcallbacks.AlarmCallbackFactory;
import org.graylog2.alarmcallbacks.AlarmCallbackHistory;
import org.graylog2.alarmcallbacks.AlarmCallbackHistoryService;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.streams.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

public class AlertNotificationsSender {
    private static final Logger LOG = LoggerFactory.getLogger(AlertNotificationsSender.class);

    private final AlarmCallbackConfigurationService alarmCallbackConfigurationService;
    private final AlarmCallbackFactory alarmCallbackFactory;
    private final AlarmCallbackHistoryService alarmCallbackHistoryService;

    @Inject
    public AlertNotificationsSender(AlarmCallbackConfigurationService alarmCallbackConfigurationService,
                                    AlarmCallbackFactory alarmCallbackFactory,
                                    AlarmCallbackHistoryService alarmCallbackHistoryService) {
        this.alarmCallbackConfigurationService = alarmCallbackConfigurationService;
        this.alarmCallbackFactory = alarmCallbackFactory;
        this.alarmCallbackHistoryService = alarmCallbackHistoryService;
    }

    public void send(AlertCondition.CheckResult result, Stream stream, Alert alert, AlertCondition alertCondition) {
        final List<AlarmCallbackConfiguration> callConfigurations = alarmCallbackConfigurationService.getForStream(stream);

        // Checking if alarm callbacks have been defined
        for (AlarmCallbackConfiguration configuration : callConfigurations) {
            AlarmCallbackHistory alarmCallbackHistory;
            AlarmCallback alarmCallback = null;
            try {
                alarmCallback = alarmCallbackFactory.create(configuration);
                alarmCallback.call(stream, result);
                alarmCallbackHistory = alarmCallbackHistoryService.success(configuration, alert, alertCondition);
            } catch (Exception e) {
                if (alarmCallback != null) {
                    LOG.warn("Alarm callback <" + alarmCallback.getName() + "> failed. Skipping.", e);
                } else {
                    LOG.warn("Alarm callback with id " + configuration.getId() + " failed. Skipping.", e);
                }
                alarmCallbackHistory = alarmCallbackHistoryService.error(configuration, alert, alertCondition, e.getMessage());
            }

            try {
                alarmCallbackHistoryService.save(alarmCallbackHistory);
            } catch (Exception e) {
                LOG.warn("Unable to save history of alarm callback run: ", e);
            }
        }
    }
}
