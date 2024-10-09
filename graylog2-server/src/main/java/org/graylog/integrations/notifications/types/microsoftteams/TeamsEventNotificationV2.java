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
package org.graylog.integrations.notifications.types.microsoftteams;

import com.floreysoft.jmte.Engine;
import com.google.common.annotations.VisibleForTesting;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.graylog.events.notifications.EventNotification;
import org.graylog.events.notifications.EventNotificationContext;
import org.graylog.events.notifications.EventNotificationException;
import org.graylog.events.notifications.EventNotificationModelData;
import org.graylog.events.notifications.EventNotificationService;
import org.graylog.events.notifications.PermanentEventNotificationException;
import org.graylog.events.notifications.TemporaryEventNotificationException;
import org.graylog.integrations.notifications.types.util.RequestClient;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.jackson.TypeReferences;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.utilities.StringUtils;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class TeamsEventNotificationV2 implements EventNotification {

    private static final Logger LOG = LoggerFactory.getLogger(TeamsEventNotificationV2.class);
    private final EventNotificationService notificationCallbackService;
    private final Engine jsonTemplateEngine;
    private final NotificationService notificationService;
    private final ObjectMapperProvider objectMapperProvider;
    private final NodeId nodeId;
    private final RequestClient requestClient;
    private final URI httpExternalUri;

    @Inject
    public TeamsEventNotificationV2(EventNotificationService notificationCallbackService,
                                    ObjectMapperProvider objectMapperProvider,
                                    @Named("JsonSafe") Engine jsonTemplateEngine,
                                    NotificationService notificationService,
                                    NodeId nodeId, RequestClient requestClient,
                                    HttpConfiguration httpConfiguration) {
        this.notificationCallbackService = notificationCallbackService;
        this.objectMapperProvider = requireNonNull(objectMapperProvider);
        this.jsonTemplateEngine = requireNonNull(jsonTemplateEngine);
        this.notificationService = requireNonNull(notificationService);
        this.nodeId = requireNonNull(nodeId);
        this.requestClient = requireNonNull(requestClient);
        this.httpExternalUri = httpConfiguration.getHttpExternalUri();
    }

    /**
     * Execute this notification with the provided context.
     *
     * @param ctx event notification context
     * @throws EventNotificationException is thrown when execute fails
     */
    @Override
    public void execute(EventNotificationContext ctx) throws EventNotificationException {
        final TeamsEventNotificationConfigV2 config = (TeamsEventNotificationConfigV2) ctx.notificationConfig();
        LOG.debug("TeamsEventNotificationV2 backlog size in method execute is [{}]", config.backlogSize());

        try {
            final String requestBody = generateBody(ctx, config);
            requestClient.send(requestBody, config.webhookUrl());
        } catch (TemporaryEventNotificationException exp) {
            // Scheduler needs to retry a TemporaryEventNotificationException
            throw exp;
        } catch (PermanentEventNotificationException exp) {
            String errorMessage = StringUtils.f("Error sending Teams Notification ID: %s. %s", ctx.notificationId(), exp.getMessage());
            final Notification systemNotification = notificationService.buildNow()
                    .addNode(nodeId.getNodeId())
                    .addType(Notification.Type.GENERIC)
                    .addSeverity(Notification.Severity.URGENT)
                    .addDetail("title", "TeamsEventNotificationV2 Failed")
                    .addDetail("description", errorMessage);

            notificationService.publishIfFirst(systemNotification);
            throw exp;

        } catch (Exception exp) {
            throw new EventNotificationException("There was an exception triggering the TeamsEventNotification", exp);
        }
    }

    /**
     * Fills out the AdaptiveCard template with the event context and backlog (if configured).
     *
     * @param ctx    event notification context
     * @param config notification configuration
     * @return the filled out template for the Teams Adaptive Card
     * @throws PermanentEventNotificationException - throws this exception when the custom message template is invalid
     */
    @VisibleForTesting
    String generateBody(EventNotificationContext ctx, TeamsEventNotificationConfigV2 config) throws PermanentEventNotificationException {
        final List<MessageSummary> backlog = getMessageBacklog(ctx, config);
        Map<String, Object> model = getCustomMessageModel(ctx, config.type(), backlog, config.timeZone());
        try {
            return jsonTemplateEngine.transform(config.adaptiveCard(), model);
        } catch (Exception e) {
            String error = "Invalid Custom Message template.";
            LOG.error("{} [{}]", error, e.toString());
            throw new PermanentEventNotificationException(error + e, e.getCause());
        }
    }

    @VisibleForTesting
    List<MessageSummary> getMessageBacklog(EventNotificationContext ctx, TeamsEventNotificationConfigV2 config) {
        List<MessageSummary> backlog = notificationCallbackService.getBacklogForEvent(ctx);
        if (config.backlogSize() > 0 && backlog != null) {
            return backlog.stream().limit(config.backlogSize()).collect(Collectors.toList());
        }
        return backlog;
    }

    @VisibleForTesting
    Map<String, Object> getCustomMessageModel(EventNotificationContext ctx, String type, List<MessageSummary> backlog, DateTimeZone timeZone) {
        final EventNotificationModelData modelData = EventNotificationModelData.of(ctx, backlog);
        LOG.debug("Custom message model: {}", modelData);

        final Map<String, Object> objectMap = objectMapperProvider.getForTimeZone(timeZone).convertValue(modelData, TypeReferences.MAP_STRING_OBJECT);
        objectMap.put("type", type);
        objectMap.put("http_external_uri", this.httpExternalUri);

        return objectMap;
    }

    public interface Factory extends EventNotification.Factory<TeamsEventNotificationV2> {
        @Override
        TeamsEventNotificationV2 create();
    }
}
