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

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.graylog.events.notifications.EventNotificationContext;
import org.graylog.events.notifications.EventNotificationModelData;
import org.graylog.events.notifications.EventNotificationService;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.events.processor.aggregation.AggregationEventProcessorConfig;
import org.graylog.integrations.pagerduty.PagerDutyNotificationConfig;
import org.graylog.integrations.pagerduty.dto.Link;
import org.graylog.integrations.pagerduty.dto.PagerDutyMessage;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamService;

import jakarta.inject.Inject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

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
    private static final List<String> PAGER_DUTY_PRIORITIES = Arrays.asList("info", "warning", "critical");

    private final StreamService streamService;
    private final EventNotificationService eventNotificationService;

    @Inject
    MessageFactory(StreamService streamService, EventNotificationService eventNotificationService) {
        this.streamService = streamService;
        this.eventNotificationService = eventNotificationService;
    }

    public PagerDutyMessage createTriggerMessage(EventNotificationContext ctx) {
        final ImmutableList<MessageSummary> backlog = eventNotificationService.getBacklogForEvent(ctx);
        final EventNotificationModelData modelData = EventNotificationModelData.of(ctx, backlog);
        final PagerDutyNotificationConfig config = (PagerDutyNotificationConfig) ctx.notificationConfig();

        String eventTitle = modelData.eventDefinitionTitle();
        String eventPriority = PAGER_DUTY_PRIORITIES.get(0);
        int priority = ctx.eventDefinition().get().priority() - 1;
        if (priority >= 0 && priority <= 2) {
            eventPriority = PAGER_DUTY_PRIORITIES.get(priority);
        }

        List<Link> streamLinks =
                streamService
                        .loadByIds(modelData.event().sourceStreams())
                        .stream()
                        .map(stream -> buildStreamWithUrl(stream, ctx, config))
                        .collect(Collectors.toList());

        String dedupKey = "";
        if (config.customIncident()) {
            dedupKey = String.format(Locale.ROOT,
                    "%s/%s/%s", config.keyPrefix(), modelData.event().sourceStreams(), eventTitle);
        }


        Map<String, String> payload = new HashMap<String, String>();
        payload.put("summary", modelData.event().message());
        payload.put("source", "Graylog:" + modelData.event().sourceStreams());
        payload.put("severity", eventPriority);
        payload.put("timestamp", modelData.event().eventTimestamp().toString());
        payload.put("component", "GraylogAlerts");
        payload.put("group", modelData.event().sourceStreams().toString());
        payload.put("class", "alerts");

        return new PagerDutyMessage(
                config.routingKey(),
                "trigger",
                dedupKey,
                config.clientName(),
                config.clientUrl(),
                streamLinks,
                payload);
    }

    private Link buildStreamWithUrl(Stream stream, EventNotificationContext ctx, PagerDutyNotificationConfig config) {
        final String graylogUrl = config.clientUrl();
        String streamUrl =
                StringUtils.appendIfMissing(graylogUrl, "/") + "streams/" + stream.getId() + "/search";

        if (ctx.eventDefinition().isPresent()) {
            EventDefinitionDto eventDefinitionDto = ctx.eventDefinition().get();
            if (eventDefinitionDto.config() instanceof AggregationEventProcessorConfig) {
                String query =
                        ((AggregationEventProcessorConfig) eventDefinitionDto.config()).query();
                streamUrl += "?q=" + query;
            }
        }
        try {
            return new Link(new URL(streamUrl), stream.getTitle());
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Error when building the stream link URL.", e);
        }
    }
}
