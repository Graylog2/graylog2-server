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

import com.fasterxml.jackson.core.JsonProcessingException;
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

import jakarta.inject.Inject;

import java.net.URI;
import java.util.Collections;
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
    private final ObjectMapperProvider objectMapperProvider;
    private final NodeId nodeId;
    private final SlackClient slackClient;
    private final URI httpExternalUri;


    @Inject
    public SlackEventNotification(EventNotificationService notificationCallbackService,
                                  ObjectMapperProvider objectMapperProvider,
                                  Engine templateEngine,
                                  NotificationService notificationService,
                                  NodeId nodeId, SlackClient slackClient,
                                  HttpConfiguration httpConfiguration) {
        this.notificationCallbackService = notificationCallbackService;
        this.objectMapperProvider = requireNonNull(objectMapperProvider);
        this.templateEngine = requireNonNull(templateEngine);
        this.notificationService = requireNonNull(notificationService);
        this.nodeId = requireNonNull(nodeId);
        this.slackClient = requireNonNull(slackClient);
        this.httpExternalUri = httpConfiguration.getHttpExternalUri();
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
        } catch (JsonProcessingException ex) {
            String errorMessage = String.format(Locale.ENGLISH, "Error serializing Slack message object while sending the SlackEventNotification :: %s", ex.getMessage());
            LOG.error(errorMessage, ex);
            final Notification systemNotification = notificationService.buildNow()
                    .addNode(nodeId.getNodeId())
                    .addType(Notification.Type.GENERIC)
                    .addSeverity(Notification.Severity.URGENT)
                    .addDetail("title", "SlackEventNotification Failed")
                    .addDetail("description", errorMessage);

            notificationService.publishIfFirst(systemNotification);
            throw new EventNotificationException("There was an error serializing the Slack message object when sending the SlackEventNotification", ex);
        } catch (TemporaryEventNotificationException exp) {
            //scheduler needs to retry a TemporaryEventNotificationException
            throw exp;
        } catch (PermanentEventNotificationException exp) {
            String errorMessage = String.format(Locale.ENGLISH, "Error sending the SlackEventNotification :: %s", exp.getMessage());
            final Notification systemNotification = notificationService.buildNow()
                    .addNode(nodeId.getNodeId())
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

        String customMessage = null;
        String template = config.customMessage();
        if (!isNullOrEmpty(template)) {
            // If the title is not included but the channel/here still needs to be notified, add a @channel tag to the custom message
            if (!config.includeTitle() && (config.notifyChannel() || config.notifyHere())) {
                String tag = config.notifyChannel() ? "channel" : "here";
                template = StringUtils.f("@%s\n%s", tag, template);
            }
            customMessage = buildCustomMessage(ctx, config, template);
        }

        SlackMessage.Attachment attachment = SlackMessage.Attachment.builder()
                .color(config.color())
                .text(customMessage)
                .build();

        //Note: Link names if notify channel or else the channel tag will be plain text.
        boolean linkNames = config.linkNames() || config.notifyChannel() || config.notifyHere();
        String templatedChannel = buildTemplatedChannel(ctx, config, config.channel());
        String emoji = config.iconEmoji() != null ? ensureEmojiSyntax(config.iconEmoji()) : "";
        return SlackMessage.builder()
                .iconEmoji(emoji)
                .iconUrl(config.iconUrl())
                .username(config.userName())
                .text(config.includeTitle() ? buildDefaultMessage(ctx, config) : null)
                .channel(templatedChannel)
                .linkNames(linkNames)
                .attachments(isNullOrEmpty(template) ? Collections.emptySet() : Collections.singleton(attachment))
                .build();
    }

    private String ensureEmojiSyntax(final String x) {
        String emoji = x.trim();

        if (!emoji.isEmpty() && !emoji.startsWith(":")) {
            emoji = ":" + emoji;
        }

        if (!emoji.isEmpty() && !emoji.endsWith(":")) {
            emoji = emoji + ":";
        }

        return emoji;
    }

    String buildDefaultMessage(EventNotificationContext ctx, SlackEventNotificationConfig config) {
        String title = buildMessageTitle(ctx);

        // Build custom message
        String audience = "";
        if (config.notifyChannel() || config.notifyHere()) {
            audience = config.notifyChannel() ? "@channel " : "@here ";
        }
        String description = ctx.eventDefinition().map(EventDefinitionDto::description).orElse("");
        return String.format(Locale.ROOT, "%s*Alert %s* triggered:\n> %s \n", audience, title, description);
    }

    private String buildMessageTitle(EventNotificationContext ctx) {
        String eventDefinitionName = ctx.eventDefinition().map(EventDefinitionDto::title).orElse("Unnamed");
        return "_" + eventDefinitionName + "_";
    }

    String buildTemplatedChannel(EventNotificationContext ctx, SlackEventNotificationConfig config, String template) throws PermanentEventNotificationException {
        final List<MessageSummary> backlog = getMessageBacklog(ctx, config);
        Map<String, Object> model = getCustomMessageModel(ctx, config.type(), backlog, config.timeZone());
        try {
            LOG.debug("channel: template = {} model = {}", template, model);
            return templateEngine.transform(template, model);
        } catch (Exception e) {
            String error = "Invalid channel template.";
            LOG.error(error + "[{}]", e.toString());
            throw new PermanentEventNotificationException(error + e, e.getCause());
        }
    }

    String buildCustomMessage(EventNotificationContext ctx, SlackEventNotificationConfig config, String template) throws PermanentEventNotificationException {
        final List<MessageSummary> backlog = getMessageBacklog(ctx, config);
        Map<String, Object> model = getCustomMessageModel(ctx, config.type(), backlog, config.timeZone());
        try {
            LOG.debug("customMessage: template = {} model = {}", template, model);
            return templateEngine.transform(template, model);
        } catch (Exception e) {
            String error = "Invalid Custom Message template.";
            LOG.error(error + "[{}]", e.toString());
            throw new PermanentEventNotificationException(error + e, e.getCause());
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
    Map<String, Object> getCustomMessageModel(EventNotificationContext ctx, String type, List<MessageSummary> backlog, DateTimeZone timeZone) {
        EventNotificationModelData modelData = EventNotificationModelData.of(ctx, backlog);

        LOG.debug("the custom message model data is {}", modelData.toString());
        Map<String, Object> objectMap = objectMapperProvider.getForTimeZone(timeZone).convertValue(modelData, TypeReferences.MAP_STRING_OBJECT);
        objectMap.put("type", type);
        objectMap.put("http_external_uri", this.httpExternalUri);
        return objectMap;
    }

    public interface Factory extends EventNotification.Factory {
        @Override
        SlackEventNotification create();
    }


}
