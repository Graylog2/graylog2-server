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
package org.graylog.integrations.pagerduty;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.events.notifications.EventNotification;
import org.graylog.events.notifications.EventNotificationContext;
import org.graylog.events.notifications.EventNotificationException;
import org.graylog.events.notifications.PermanentEventNotificationException;
import org.graylog.events.notifications.TemporaryEventNotificationException;
import org.graylog.integrations.pagerduty.client.MessageFactory;
import org.graylog.integrations.pagerduty.client.PagerDutyClient;
import org.graylog.integrations.pagerduty.dto.PagerDutyResponse;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.system.NodeId;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

/**
 * Main class that focuses on event notifications that should be sent to PagerDuty.
 *
 * @author Edgar Molina
 *
 */
public class PagerDutyNotification implements EventNotification
{
    private final PagerDutyClient pagerDutyClient;
    private final MessageFactory messageFactory;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final NodeId nodeId;

    @Inject
    PagerDutyNotification(PagerDutyClient pagerDutyClient,
                          MessageFactory messageFactory,
                          ObjectMapper objectMapper,
                          NotificationService notificationService,
                          NodeId nodeId) {
        this.pagerDutyClient = pagerDutyClient;
        this.messageFactory = messageFactory;
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
        this.nodeId = nodeId;
    }

    public interface Factory extends EventNotification.Factory {
        @Override
        PagerDutyNotification create();
    }

    @Override
    public void execute(EventNotificationContext ctx) throws EventNotificationException {
        final String payloadString = buildRequestBody(ctx);
        try {
            PagerDutyResponse response = pagerDutyClient.enqueue(payloadString);
            List<String> errors = response.getErrors();
            if (errors != null && errors.size() > 0) {
                throw new IllegalStateException(
                        "There was an error triggering the PagerDuty event, details: " + errors);
            }
        } catch (PagerDutyClient.TemporaryPagerDutyClientException e) {
            throw new TemporaryEventNotificationException(
                    String.format("Error enqueueing the PagerDuty event :: %s", e.getMessage()),
                    null != e.getCause() ? e.getCause() : e);
        } catch (PagerDutyClient.PermanentPagerDutyClientException e) {
            String errorMessage = String.format("Error enqueueing the PagerDuty event :: %s", e.getMessage());
            Notification systemNotification = notificationService.buildNow()
                    .addNode(nodeId.toString())
                    .addType(Notification.Type.GENERIC)
                    .addSeverity(Notification.Severity.URGENT)
                    .addDetail("title", "PagerDuty Notification Failed")
                    .addDetail("description", errorMessage);
            notificationService.publishIfFirst(systemNotification);
            throw new PermanentEventNotificationException(
                    errorMessage,
                    null != e.getCause() ? e.getCause() : e);
        } catch (Throwable t) {
            throw new EventNotificationException ("There was an exception triggering the PagerDuty event.", t);
        }
    }

    private String buildRequestBody(EventNotificationContext ctx) throws PermanentEventNotificationException {
        try {
            return objectMapper.writeValueAsString(messageFactory.createTriggerMessage(ctx));
        } catch (IOException e) {
            throw new PermanentEventNotificationException("Failed to build payload for PagerDuty API", e);
        }
    }
}