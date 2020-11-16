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
package org.graylog.events.notifications.types;

import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.util.Strings;
import org.graylog.events.notifications.EventNotification;
import org.graylog.events.notifications.EventNotificationContext;
import org.graylog.events.notifications.EventNotificationService;
import org.graylog.events.notifications.PermanentEventNotificationException;
import org.graylog.events.notifications.TemporaryEventNotificationException;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.plugin.alarms.transports.TransportConfigurationException;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class EmailEventNotification implements EventNotification {
    public interface Factory extends EventNotification.Factory {
        @Override
        EmailEventNotification create();
    }

    private static final Logger LOG = LoggerFactory.getLogger(EmailEventNotification.class);

    private final EventNotificationService notificationCallbackService;
    private final EmailSender emailSender;
    private final NotificationService notificationService;
    private final NodeId nodeId;

    @Inject
    public EmailEventNotification(EventNotificationService notificationCallbackService,
                                  EmailSender emailSender,
                                  NotificationService notificationService,
                                  NodeId nodeId) {
        this.notificationCallbackService = notificationCallbackService;
        this.emailSender = emailSender;
        this.notificationService = notificationService;
        this.nodeId = nodeId;
    }

    @Override
    public void execute(EventNotificationContext ctx) throws TemporaryEventNotificationException, PermanentEventNotificationException {
        final EmailEventNotificationConfig config = (EmailEventNotificationConfig) ctx.notificationConfig();

        try {
            ImmutableList<MessageSummary> backlog = notificationCallbackService.getBacklogForEvent(ctx);
            emailSender.sendEmails(config, ctx, backlog);
        } catch (EmailSender.ConfigurationError e) {
            throw new TemporaryEventNotificationException(e.getMessage());
        } catch (TransportConfigurationException e) {
            Notification systemNotification = notificationService.buildNow()
                    .addNode(nodeId.toString())
                    .addType(Notification.Type.EMAIL_TRANSPORT_CONFIGURATION_INVALID)
                    .addSeverity(Notification.Severity.NORMAL)
                    .addDetail("exception", e.getMessage());
            notificationService.publishIfFirst(systemNotification);

            throw new TemporaryEventNotificationException("Notification has email recipients and is triggered, but email transport is not configured. " +
                    e.getMessage());
        } catch (Exception e) {
            String exceptionDetail = e.toString();
            if (e.getCause() != null) {
                exceptionDetail += " (" + e.getCause() + ")";
            }

            final Notification systemNotification = notificationService.buildNow()
                    .addNode(nodeId.toString())
                    .addType(Notification.Type.EMAIL_TRANSPORT_FAILED)
                    .addSeverity(Notification.Severity.NORMAL)
                    .addDetail("exception", exceptionDetail);
            notificationService.publishIfFirst(systemNotification);

            throw new PermanentEventNotificationException("Notification has email recipients and is triggered, but sending emails failed. " +
                    e.getMessage());
        }

        LOG.debug("Sending email to addresses <{}> and users <{}> using notification <{}>",
                Strings.join(config.emailRecipients(), ','),
                Strings.join(config.userRecipients(), ','),
                ctx.notificationId());
    }
}
