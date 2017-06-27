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

import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;

public class AlertScanner {
    private static final Logger LOG = LoggerFactory.getLogger(AlertScanner.class);

    private final AlertService alertService;
    private final AlertNotificationsSender alertNotificationsSender;

    @Inject
    public AlertScanner(AlertService alertService, AlertNotificationsSender alertNotificationsSender) {
        this.alertService = alertService;
        this.alertNotificationsSender = alertNotificationsSender;
    }

    private Alert handleTriggeredAlert(AlertCondition.CheckResult result, Stream stream, AlertCondition alertCondition) throws ValidationException {
        // Persist alert.
        final Alert alert = alertService.factory(result);
        alertService.save(alert);

        alertNotificationsSender.send(result, stream, alert, alertCondition);

        return alert;
    }

    private void handleRepeatedAlert(Stream stream, AlertCondition alertCondition, AlertCondition.CheckResult result, Alert alert2) {
        alertNotificationsSender.send(result, stream, alert2, alertCondition);
    }

    private void handleResolveAlert(Alert alert) {
        alertService.resolveAlert(alert);
        // TODO: Send resolve notifications
    }

    public boolean checkAlertCondition(Stream stream, AlertCondition alertCondition) {
        if (stream.isPaused() || alertService.inGracePeriod(alertCondition)) {
            return false;
        }
        try {
            final AlertCondition.CheckResult result = alertCondition.runCheck();
            final Optional<Alert> alert = alertService.getLastTriggeredAlert(stream.getId(), alertCondition.getId());
            if (result.isTriggered()) {
                if (!alert.isPresent() || alertService.isResolved(alert.get())) {
                    // Alert is triggered for the first time
                    LOG.debug("Alert condition [{}] is triggered. Sending alerts.", alertCondition);
                    handleTriggeredAlert(result, stream, alertCondition);
                } else {
                    final Alert triggeredAlert = alert.get();
                    // There is already an alert for this condition and is unresolved
                    if (alertService.shouldRepeatNotifications(alertCondition, triggeredAlert)) {
                        // Repeat notifications because user wants to do that
                        LOG.debug("Alert condition [{}] is triggered and configured to repeat alert notifications. Sending alerts.", alertCondition);
                        handleRepeatedAlert(stream, alertCondition, result, triggeredAlert);
                    } else {
                        LOG.debug("Alert condition [{}] is triggered but alerts were already sent. Nothing to do.", alertCondition);
                    }
                }
                return true;
            } else {
                // if stream and condition had already an alert, mark it as resolved
                if (alert.isPresent() && !alertService.isResolved(alert.get())) {
                    LOG.debug("Alert condition [{}] is not triggered anymore. Resolving alert.", alertCondition);
                    handleResolveAlert(alert.get());
                } else {
                    LOG.debug("Alert condition [{}] is not triggered and is marked as resolved. Nothing to do.", alertCondition);
                }
            }
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.error("Skipping alert check <{}/{}>", alertCondition.getTitle(), alertCondition.getId(), e);
            } else {
                LOG.error("Skipping alert check <{}/{}>: {} ({})", alertCondition.getTitle(),
                        alertCondition.getId(), e.getMessage(), e.getClass().getSimpleName());
            }
        }
        return false;
    }
}
