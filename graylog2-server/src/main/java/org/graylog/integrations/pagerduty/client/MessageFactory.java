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
package org.graylog.integrations.pagerduty.client;

import com.floreysoft.jmte.Engine;
import com.google.common.collect.ImmutableList;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.commons.lang3.Strings;
import org.graylog.events.notifications.EventNotificationContext;
import org.graylog.events.notifications.EventNotificationModelData;
import org.graylog.events.notifications.EventNotificationService;
import org.graylog.events.notifications.TemplateModelProvider;
import org.graylog.integrations.pagerduty.PagerDutyNotificationConfig;
import org.graylog.integrations.pagerduty.dto.Link;
import org.graylog.integrations.pagerduty.dto.PagerDutyMessage;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.web.customization.CustomizationConfig;
import org.joda.time.DateTimeZone;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Factory class for PagerDuty messages, heavily based on the works of the cited authors.
 *
 * @author Jochen Schalanda
 * @author James Carr
 * @author Dennis Oelkers
 * @author Padma Liyanage
 * @author Edgar Molina
 */
public class MessageFactory {
    private static final List<String> PAGER_DUTY_PRIORITIES = Arrays.asList("info", "warning", "critical", "critical");

    private final EventNotificationService eventNotificationService;
    private final CustomizationConfig customizationConfig;
    private final Engine templateEngine;
    private final TemplateModelProvider templateModelProvider;

    @Inject
    MessageFactory(EventNotificationService eventNotificationService,
                   CustomizationConfig customizationConfig,
                   @Named("JsonSafe") Engine jsonTemplateEngine,
                   TemplateModelProvider templateModelProvider) {
        this.eventNotificationService = eventNotificationService;
        this.customizationConfig = customizationConfig;
        this.templateEngine = jsonTemplateEngine;
        this.templateModelProvider = templateModelProvider;
    }

    public PagerDutyMessage createTriggerMessage(EventNotificationContext ctx) {
        final ImmutableList<MessageSummary> backlog = eventNotificationService.getBacklogForEvent(ctx);
        final EventNotificationModelData modelData = EventNotificationModelData.of(ctx, backlog);
        final PagerDutyNotificationConfig config = (PagerDutyNotificationConfig) ctx.notificationConfig();
        final Map<String, Object> messageModel = getCustomMessageModel(ctx, backlog);

        final String eventTitle = config.pagerDutyTitle()
                .map(customTitle -> templateEngine.transform(customTitle, messageModel))
                .orElse(modelData.eventDefinitionTitle());
        String eventPriority = PAGER_DUTY_PRIORITIES.get(0);
        int priority = ctx.eventDefinition().get().priority() - 1;
        if (priority >= 0 && priority <= 3) {
            eventPriority = PAGER_DUTY_PRIORITIES.get(priority);
        }

        final List<Link> replayLink;
        try {
            final String replayUrl = Strings.CS.appendIfMissing(
                    config.clientUrl(), "/") + "alerts/" + modelData.event().id() + "/replay-search";
            replayLink = List.of(new Link(new URI(replayUrl).toURL(), "Replay Event"));
        } catch (URISyntaxException | MalformedURLException e) {
            throw new IllegalStateException("Error when building the event replay URL.", e);
        }

        String dedupKey = "";
        if (config.customIncident()) {
            final String formattedPrefix = templateEngine.transform(config.keyPrefix(), messageModel);
            final String prefixedIncidentKey = String.format(Locale.ROOT,
                    "%s/%s/%s", formattedPrefix, modelData.event().sourceStreams(), eventTitle);
            // Use the custom incident key if provided, otherwise fall back to the prefixed key.
            dedupKey = config.incidentKey()
                    .map(incidentKeyTemplate -> templateEngine.transform(incidentKeyTemplate, messageModel))
                    .orElse(prefixedIncidentKey);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("summary", config.pagerDutyTitle()
                .map(customTitle -> templateEngine.transform(customTitle, messageModel))
                .orElse(modelData.event().message()));
        payload.put("source", customizationConfig.productName() + ":" + modelData.event().sourceStreams());
        payload.put("severity", eventPriority);
        payload.put("timestamp", modelData.event().eventTimestamp().toString());
        payload.put("component", "GraylogAlerts");
        payload.put("group", modelData.event().sourceStreams().toString());
        payload.put("class", "alerts");
        payload.put("custom_details", ctx.event().fields());

        return new PagerDutyMessage(
                config.routingKey(),
                "trigger",
                dedupKey,
                config.clientName(),
                config.clientUrl(),
                replayLink,
                payload);
    }

    private Map<String, Object> getCustomMessageModel(EventNotificationContext ctx, List<MessageSummary> backlog) {
        return templateModelProvider.of(ctx, backlog, DateTimeZone.UTC, Map.of("type", PagerDutyNotificationConfig.TYPE_NAME));
    }
}
