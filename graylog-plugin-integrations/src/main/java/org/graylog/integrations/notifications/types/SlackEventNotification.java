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
package org.graylog.integrations.notifications.types;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.graylog2.jackson.TypeReferences;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;

public class SlackEventNotification implements EventNotification {

    private static final Logger LOG = LoggerFactory.getLogger(SlackEventNotification.class);
    private final EventNotificationService notificationCallbackService;
    private final Engine templateEngine;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final NodeId nodeId;
    private final SlackClient slackClient;

    @Inject
    public SlackEventNotification(EventNotificationService notificationCallbackService,
                                  ObjectMapper objectMapper,
                                  Engine templateEngine,
                                  NotificationService notificationService,
                                  NodeId nodeId, SlackClient slackClient) {
        this.notificationCallbackService = notificationCallbackService;
        this.objectMapper = requireNonNull(objectMapper);
        this.templateEngine = requireNonNull(templateEngine);
        this.notificationService = requireNonNull(notificationService);
        this.nodeId = requireNonNull(nodeId);
        this.slackClient = requireNonNull(slackClient);
    }

    /**
     * @param ctx
     * @throws EventNotificationException is thrown when execute fails
     */
    @Override
    public void execute(EventNotificationContext ctx) throws EventNotificationException {
        final SlackEventNotificationConfig config = (SlackEventNotificationConfig) ctx.notificationConfig();
        LOG.debug("SlackEventNotification backlog size in method execute is [{}]", config.backlogSize());

        try {
            SlackMessage slackMessage = createSlackMessage(ctx, config);
            slackClient.send(slackMessage, config.webhookUrl());
        } catch (TemporaryEventNotificationException exp) {
            //scheduler needs to retry a TemporaryEventNotificationException
            throw exp;
        } catch (PermanentEventNotificationException exp) {
            String errorMessage = String.format("Error sending the SlackEventNotification :: %s", exp.getMessage());
            final Notification systemNotification = notificationService.buildNow()
                    .addNode(nodeId.toString())
                    .addType(Notification.Type.GENERIC)
                    .addSeverity(Notification.Severity.URGENT)
                    .addDetail("title", "SlackEventNotification Failed")
                    .addDetail("description", errorMessage);

            notificationService.publishIfFirst(systemNotification);
            throw exp;

        } catch (Exception exp) {
            throw new EventNotificationException("There was an exception triggering the SlackEventNotification", exp);
        }
    }

    /**
     * @param ctx
     * @param config
     * @return
     * @throws PermanentEventNotificationException - throws this exception when the custom message template is invalid
     */
    SlackMessage createSlackMessage(EventNotificationContext ctx, SlackEventNotificationConfig config) throws PermanentEventNotificationException {
        //Note: Link names if notify channel or else the channel tag will be plain text.
        boolean linkNames = config.linkNames() || config.notifyChannel();
        String message = buildDefaultMessage(ctx, config);

        String customMessage = null;
        String template = config.customMessage();
        if (!isNullOrEmpty(template)) {
            customMessage = buildCustomMessage(ctx, config, template);
        }

        return new SlackMessage(
                config.color(),
                config.iconEmoji(),
                config.iconUrl(),
                config.userName(),
                config.channel(),
                linkNames,
                message,
                customMessage
        );
    }

    String buildDefaultMessage(EventNotificationContext ctx, SlackEventNotificationConfig config) {
        String title = buildMessageTitle(ctx);

        // Build custom message
        String audience = config.notifyChannel() ? "@channel " : "";
        String description = ctx.eventDefinition().map(EventDefinitionDto::description).orElse("");
        return String.format(Locale.ROOT, "%s*Alert %s* triggered:\n> %s \n", audience, title, description);
    }

    private String buildMessageTitle(EventNotificationContext ctx) {
        String eventDefinitionName = ctx.eventDefinition().map(EventDefinitionDto::title).orElse("Unnamed");
        return "_" + eventDefinitionName + "_";
    }

    String buildCustomMessage(EventNotificationContext ctx, SlackEventNotificationConfig config, String template) throws PermanentEventNotificationException {
        final List<MessageSummary> backlog = getMessageBacklog(ctx, config);
        Map<String, Object> model = getCustomMessageModel(ctx, config.type(), backlog);
        try {
            LOG.debug("template = {} model = {}", template, model);
            return templateEngine.transform(template, model);
        } catch (Exception e) {
            String error = "Invalid Custom Message template.";
            LOG.error(error + "[{}]", e.toString());
            throw new PermanentEventNotificationException(error + e.toString(), e.getCause());
        }
    }

    @VisibleForTesting
    List<MessageSummary> getMessageBacklog(EventNotificationContext ctx, SlackEventNotificationConfig config) {
        List<MessageSummary> backlog = notificationCallbackService.getBacklogForEvent(ctx);
        if (config.backlogSize() > 0 && backlog != null) {
            return backlog.stream().limit(config.backlogSize()).collect(Collectors.toList());
        }
        return backlog;
    }


    @VisibleForTesting
    Map<String, Object> getCustomMessageModel(EventNotificationContext ctx, String type, List<MessageSummary> backlog) {
        EventNotificationModelData modelData = EventNotificationModelData.of(ctx, backlog);

        LOG.debug("the custom message model data is {}", modelData.toString());
        Map<String, Object> objectMap = objectMapper.convertValue(modelData, TypeReferences.MAP_STRING_OBJECT);
        objectMap.put("type", type);
        return objectMap;
    }

    public interface Factory extends EventNotification.Factory {
        @Override
        SlackEventNotification create();
    }


}
