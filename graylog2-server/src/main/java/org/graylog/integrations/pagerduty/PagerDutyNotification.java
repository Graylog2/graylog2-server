/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
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

import jakarta.inject.Inject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Main class that focuses on event notifications that should be sent to PagerDuty.
 *
 * @author Edgar Molina
 */
public class PagerDutyNotification implements EventNotification {
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
                    String.format(Locale.ROOT, "Error enqueueing the PagerDuty event :: %s", e.getMessage()),
                    null != e.getCause() ? e.getCause() : e);
        } catch (PagerDutyClient.PermanentPagerDutyClientException e) {
            String errorMessage = String.format(Locale.ROOT, "Error enqueueing the PagerDuty event :: %s", e.getMessage());
            Notification systemNotification = notificationService.buildNow()
                    .addNode(nodeId.getNodeId())
                    .addType(Notification.Type.GENERIC)
                    .addSeverity(Notification.Severity.URGENT)
                    .addDetail("title", "PagerDuty Notification Failed")
                    .addDetail("description", errorMessage);
            notificationService.publishIfFirst(systemNotification);
            throw new PermanentEventNotificationException(
                    errorMessage,
                    null != e.getCause() ? e.getCause() : e);
        } catch (Throwable t) {
            throw new EventNotificationException("There was an exception triggering the PagerDuty event.", t);
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
