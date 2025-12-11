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
import com.google.common.collect.ImmutableSet;
import org.graylog.events.event.EventDto;
import org.graylog.events.notifications.EventNotificationContext;
import org.graylog.events.notifications.EventNotificationService;
import org.graylog.events.notifications.NotificationDto;
import org.graylog.events.notifications.NotificationTestData;
import org.graylog.events.notifications.TemplateModelProvider;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.integrations.pagerduty.PagerDutyNotificationConfig;
import org.graylog.integrations.pagerduty.dto.PagerDutyMessage;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.web.customization.CustomizationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageFactoryTest {

    private static final String ROUTING_KEY = "12345678901234567890123456789012";
    private static final String CLIENT_NAME = "client";
    private static final String CLIENT_URL = "http://localhost/";

    @Mock
    private EventNotificationService eventNotificationService;

    private TemplateModelProvider templateModelProvider;
    private MessageFactory messageFactory;

    @BeforeEach
    void setUp() {
        templateModelProvider = new TemplateModelProvider(CustomizationConfig.empty(), new ObjectMapperProvider(), new HttpConfiguration());
        messageFactory = new MessageFactory(
                eventNotificationService,
                CustomizationConfig.empty(),
                Engine.createEngine(),
                templateModelProvider);

        when(eventNotificationService.getBacklogForEvent(any(EventNotificationContext.class))).thenReturn(ImmutableList.<MessageSummary>of());
    }

    @Test
    void usesCustomIncidentKeyWhenProvided() {
        final PagerDutyNotificationConfig config = baseConfigBuilder()
                .customIncident(true)
                .keyPrefix("prefix")
                .incidentKey("test/${event.id}")
                .build();

        final PagerDutyMessage message = messageFactory.createTriggerMessage(buildContext(config));

        assertThat(message.getDedupKey()).isEqualTo("test/TEST_NOTIFICATION_ID");
    }

    @Test
    void createsEmptyDedupKeyWhenCustomIncidentDisabled() {
        final PagerDutyNotificationConfig config = baseConfigBuilder().build();

        final PagerDutyMessage message = messageFactory.createTriggerMessage(buildContext(config));

        assertThat(message.getDedupKey()).isEmpty();
        assertThat(message.getPayload().get("summary")).isEqualTo("Notification test message triggered from user <user>");
    }

    @Test
    void buildsPrefixedIncidentKeyWhenTemplateMissing() {
        final PagerDutyNotificationConfig config = baseConfigBuilder()
                .customIncident(true)
                .keyPrefix("prefix")
                .build();

        final EventNotificationContext baseContext = buildContext(config);
        final EventDto event = baseContext.event().toBuilder()
                .sourceStreams(ImmutableSet.of("stream-one"))
                .build();
        final EventNotificationContext context = baseContext.toBuilder()
                .event(event)
                .build();

        final PagerDutyMessage message = messageFactory.createTriggerMessage(context);

        assertThat(message.getDedupKey()).isEqualTo("prefix/[stream-one]/Event Definition Test Title");
    }

    @Test
    void derivesSeverityFromEventDefinitionPriority() {
        final PagerDutyNotificationConfig config = baseConfigBuilder().build();
        final EventNotificationContext context = buildContext(config);
        final EventDefinitionDto eventDefinition = context.eventDefinition().orElseThrow().toBuilder()
                .priority(4)
                .build();

        final PagerDutyMessage message = messageFactory.createTriggerMessage(context.toBuilder()
                .eventDefinition(eventDefinition)
                .build());

        assertThat(message.getPayload().get("severity")).isEqualTo("critical");
    }

    @Test
    void defaultsToInfoSeverityWhenPriorityOutOfBounds() {
        final PagerDutyNotificationConfig config = baseConfigBuilder().build();
        final EventNotificationContext context = buildContext(config);
        final EventDefinitionDto eventDefinition = context.eventDefinition().orElseThrow().toBuilder()
                .priority(6)
                .build();

        final PagerDutyMessage message = messageFactory.createTriggerMessage(context.toBuilder()
                .eventDefinition(eventDefinition)
                .build());

        assertThat(message.getPayload().get("severity")).isEqualTo("info");
    }

    @Test
    void usesCustomPagerDutyTitleAsSummary() {
        final PagerDutyNotificationConfig config = baseConfigBuilder()
                .pagerDutyTitle("Custom ${event_definition_title}")
                .build();

        final PagerDutyMessage message = messageFactory.createTriggerMessage(buildContext(config));

        assertThat(message.getPayload().get("summary")).isEqualTo("Custom Event Definition Test Title");
    }

    @Test
    void createsReplayLinkUsingClientUrlAndEventId() {
        final PagerDutyNotificationConfig config = baseConfigBuilder()
                .clientUrl("http://pagerduty.example.com")
                .build();

        final PagerDutyMessage message = messageFactory.createTriggerMessage(buildContext(config));

        assertThat(message.getLinks()).hasSize(1);
        assertThat(message.getLinks().get(0).getText()).isEqualTo("Replay Event");
        assertThat(message.getLinks().get(0).getHref().toString())
                .isEqualTo("http://pagerduty.example.com/alerts/TEST_NOTIFICATION_ID/replay-search");
    }

    @Test
    void populatesCustomDetailsWithEventFields() {
        final PagerDutyNotificationConfig config = baseConfigBuilder().build();
        final EventNotificationContext context = buildContext(config);

        final PagerDutyMessage message = messageFactory.createTriggerMessage(context);

        assertThat(message.getPayload().get("custom_details")).isEqualTo(context.event().fields());
    }

    private PagerDutyNotificationConfig.Builder baseConfigBuilder() {
        return PagerDutyNotificationConfig.builder()
                .routingKey(ROUTING_KEY)
                .customIncident(false)
                .keyPrefix("")
                .clientName(CLIENT_NAME)
                .clientUrl(CLIENT_URL)
                .pagerDutyTitle(null)
                .incidentKey(null);
    }

    private EventNotificationContext buildContext(PagerDutyNotificationConfig config) {
        final NotificationDto notificationDto = NotificationDto.builder()
                .id("notification-id")
                .title("title")
                .description("desc")
                .config(config)
                .build();

        return NotificationTestData.getDummyContext(notificationDto, "user")
                .toBuilder()
                .notificationConfig(config)
                .build();
    }
}
