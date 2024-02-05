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
import org.graylog.events.notifications.EventNotification;
import org.graylog.events.notifications.EventNotificationContext;
import org.graylog.events.notifications.EventNotificationException;
import org.graylog.events.notifications.EventNotificationModelData;
import org.graylog.events.notifications.EventNotificationService;
import org.graylog.events.notifications.PermanentEventNotificationException;
import org.graylog.events.notifications.TemporaryEventNotificationException;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.integrations.notifications.types.util.RequestClient;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.jackson.TypeReferences;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;

public class TeamsEventNotification implements EventNotification {

    private static final Logger LOG = LoggerFactory.getLogger(TeamsEventNotification.class);
    private final EventNotificationService notificationCallbackService;
    private final Engine templateEngine;
    private final NotificationService notificationService;
    private final ObjectMapperProvider objectMapperProvider;
    private final NodeId nodeId;
    private final RequestClient requestClient;
    private final URI httpExternalUri;

    @Inject
    public TeamsEventNotification(EventNotificationService notificationCallbackService,
                                  ObjectMapperProvider objectMapperProvider,
                                  Engine templateEngine,
                                  NotificationService notificationService,
                                  NodeId nodeId, RequestClient requestClient,
                                  HttpConfiguration httpConfiguration) {
        this.notificationCallbackService = notificationCallbackService;
        this.objectMapperProvider = requireNonNull(objectMapperProvider);
        this.templateEngine = requireNonNull(templateEngine);
        this.notificationService = requireNonNull(notificationService);
        this.nodeId = requireNonNull(nodeId);
        this.requestClient = requireNonNull(requestClient);
        this.httpExternalUri = httpConfiguration.getHttpExternalUri();
    }

    /**
     * @param ctx
     * @throws EventNotificationException is thrown when execute fails
     */
    @Override
    public void execute(EventNotificationContext ctx) throws EventNotificationException {
        final TeamsEventNotificationConfig config = (TeamsEventNotificationConfig) ctx.notificationConfig();
        LOG.debug("TeamsEventNotification backlog size in method execute is [{}]", config.backlogSize());

        try {
            TeamsMessage teamsMessage = createTeamsMessage(ctx, config);
            requestClient.send(objectMapperProvider.getForTimeZone(config.timeZone()).writeValueAsString(teamsMessage), config.webhookUrl());
        } catch (TemporaryEventNotificationException exp) {
            //scheduler needs to retry a TemporaryEventNotificationException
            throw exp;
        } catch (PermanentEventNotificationException exp) {
            String errorMessage = String.format(Locale.ROOT, "Error sending the TeamsEventNotification :: %s", exp.getMessage());
            final Notification systemNotification = notificationService.buildNow()
                    .addNode(nodeId.getNodeId())
                    .addType(Notification.Type.GENERIC)
                    .addSeverity(Notification.Severity.URGENT)
                    .addDetail("title", "TeamsEventNotification Failed")
                    .addDetail("description", errorMessage);

            notificationService.publishIfFirst(systemNotification);
            throw exp;

        } catch (Exception exp) {
            throw new EventNotificationException("There was an exception triggering the TeamsEventNotification", exp);
        }
    }

    /**
     * @param ctx
     * @param config
     * @return
     * @throws PermanentEventNotificationException - throws this exception when the custom message template is invalid
     */
    TeamsMessage createTeamsMessage(EventNotificationContext ctx, TeamsEventNotificationConfig config) throws PermanentEventNotificationException {
        String messageTitle = buildDefaultMessage(ctx);
        String description = buildMessageDescription(ctx);
        String customMessage = null;
        String template = config.customMessage();
        String summary = ctx.eventDefinition().map(EventDefinitionDto::title).orElse("Graylog Event");
        if (!isNullOrEmpty(template)) {
            customMessage = buildCustomMessage(ctx, config, template);
        }

        TeamsMessage.Sections section = TeamsMessage.Sections.builder()
                .activityImage(config.iconUrl())
                .activitySubtitle(description)
                .text(customMessage)
                .build();

        return TeamsMessage.builder()
                .color(config.color())
                .text(messageTitle)
                .summary(summary)
                .sections(Collections.singleton(section))
                .build();
    }

    String buildDefaultMessage(EventNotificationContext ctx) {
        String title = ctx.eventDefinition().map(EventDefinitionDto::title).orElse("Unnamed");

        // Build Message title
        return String.format(Locale.ROOT, "**Alert %s triggered:**\n", title);
    }

    private String buildMessageDescription(EventNotificationContext ctx) {
        String description = ctx.eventDefinition().map(EventDefinitionDto::description).orElse("");
        return "_" + description + "_";
    }


    String buildCustomMessage(EventNotificationContext ctx, TeamsEventNotificationConfig config, String template) throws PermanentEventNotificationException {
        final List<MessageSummary> backlog = getMessageBacklog(ctx, config);
        Map<String, Object> model = getCustomMessageModel(ctx, config.type(), backlog, config.timeZone());
        try {
            return templateEngine.transform(template, model);
        } catch (Exception e) {
            String error = "Invalid Custom Message template.";
            LOG.error("{} [{}]", error, e.toString());
            throw new PermanentEventNotificationException(error + e, e.getCause());
        }
    }

    @VisibleForTesting
    List<MessageSummary> getMessageBacklog(EventNotificationContext ctx, TeamsEventNotificationConfig config) {
        List<MessageSummary> backlog = notificationCallbackService.getBacklogForEvent(ctx);
        if (config.backlogSize() > 0 && backlog != null) {
            return backlog.stream().limit(config.backlogSize()).collect(Collectors.toList());
        }
        return backlog;
    }


    @VisibleForTesting
    Map<String, Object> getCustomMessageModel(EventNotificationContext ctx, String type, List<MessageSummary> backlog, DateTimeZone timeZone) {
        EventNotificationModelData modelData = EventNotificationModelData.of(ctx, backlog);

        LOG.debug("the custom message model data is {}", modelData);
        Map<String, Object> objectMap = objectMapperProvider.getForTimeZone(timeZone).convertValue(modelData, TypeReferences.MAP_STRING_OBJECT);
        objectMap.put("type", type);
        objectMap.put("http_external_uri", this.httpExternalUri);

        return objectMap;
    }

    public interface Factory extends EventNotification.Factory<TeamsEventNotification> {
        @Override
        TeamsEventNotification create();
    }

}
