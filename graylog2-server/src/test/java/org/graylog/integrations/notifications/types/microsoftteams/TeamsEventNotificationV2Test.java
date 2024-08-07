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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog.events.event.EventDto;
import org.graylog.events.notifications.EventNotificationContext;
import org.graylog.events.notifications.EventNotificationException;
import org.graylog.events.notifications.EventNotificationService;
import org.graylog.events.notifications.NotificationDto;
import org.graylog.events.notifications.NotificationTestData;
import org.graylog.events.notifications.PermanentEventNotificationException;
import org.graylog.events.notifications.TemporaryEventNotificationException;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.integrations.notifications.types.util.RequestClient;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.notifications.NotificationImpl;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TeamsEventNotificationV2Test {

    // Code under test
    TeamsEventNotificationV2 teamsEventNotification;

    private final NodeId nodeId = new SimpleNodeId("12345");
    private final MessageFactory messageFactory = new TestMessageFactory();

    @Mock
    NotificationService mockNotificationService;

    @Mock
    RequestClient mockrequestClient;

    @Mock
    EventNotificationService notificationCallbackService;

    private TeamsEventNotificationConfigV2 notificationConfig;
    private EventNotificationContext eventNotificationContext;

    @Before
    public void setUp() {
        getDummyTeamsNotificationConfig();
        eventNotificationContext = NotificationTestData.getDummyContext(getNotificationDto(), "unit_tests").toBuilder().build();
        final ImmutableList<MessageSummary> messageSummaries = generateMessageSummaries(50);
        when(notificationCallbackService.getBacklogForEvent(eventNotificationContext)).thenReturn(messageSummaries);

        teamsEventNotification = new TeamsEventNotificationV2(notificationCallbackService,
                new ObjectMapperProvider(),
                Engine.createEngine(),
                mockNotificationService,
                nodeId,
                mockrequestClient,
                new HttpConfiguration());
    }

    @Test
    public void testEscapedQuotes() {
        if (eventNotificationContext.eventDefinition().isPresent()) {
            EventDefinitionDto definition = eventNotificationContext.eventDefinition().get();
            definition = definition.toBuilder().description("A Description with \"Double Quotes\"").build();
            eventNotificationContext = eventNotificationContext.toBuilder().eventDefinition(definition).build();
        }
        List<MessageSummary> messageSummaries = generateMessageSummaries(50);
        Map<String, Object> customMessageModel = teamsEventNotification.getCustomMessageModel(eventNotificationContext, notificationConfig.type(), messageSummaries, DateTimeZone.UTC);
        assertThat(customMessageModel.get("event_definition_description")).isEqualTo("A Description with \\\"Double Quotes\\\"");
    }

    @Test
    public void getCustomMessageModel() {
        List<MessageSummary> messageSummaries = generateMessageSummaries(50);
        Map<String, Object> customMessageModel = teamsEventNotification.getCustomMessageModel(eventNotificationContext, notificationConfig.type(), messageSummaries, DateTimeZone.UTC);

        assertThat(customMessageModel).isNotNull();
        assertThat(customMessageModel.get("event_definition_description")).isEqualTo("Event Definition Test Description");
        assertThat(customMessageModel.get("event_definition_title")).isEqualTo("Event Definition Test Title");
        assertThat(customMessageModel.get("event_definition_type")).isEqualTo("test-dummy-v1");
        assertThat(customMessageModel.get("type")).isEqualTo(TeamsEventNotificationConfigV2.TYPE_NAME);
        assertThat(customMessageModel.get("job_definition_id")).isEqualTo("<unknown>");
        assertThat(customMessageModel.get("job_trigger_id")).isEqualTo("<unknown>");
    }

    @Test(expected = EventNotificationException.class)
    public void executeWithInvalidWebhookUrl() throws EventNotificationException {
        givenGoodNotificationService();
        givenTeamsClientThrowsPermException();
        // When execute is called with an invalid webhook URL, we expect an event notification exception.
        teamsEventNotification.execute(eventNotificationContext);
    }

    @Test(expected = EventNotificationException.class)
    public void executeWithNullEventTimerange() throws EventNotificationException {
        EventNotificationContext yetAnotherContext = getEventNotificationContextToSimulateNullPointerException();
        assertThat(yetAnotherContext.event().timerangeStart().isPresent()).isFalse();
        assertThat(yetAnotherContext.event().timerangeEnd().isPresent()).isFalse();
        assertThat(yetAnotherContext.notificationConfig().type()).isEqualTo(TeamsEventNotificationConfigV2.TYPE_NAME);
        teamsEventNotification.execute(yetAnotherContext);
    }

    @Test(expected = PermanentEventNotificationException.class)
    public void buildCustomMessageWithInvalidTemplate() throws EventNotificationException {
        notificationConfig = buildInvalidTemplate();
        teamsEventNotification.generateBody(eventNotificationContext, notificationConfig);
    }

    @Test
    public void testBacklogMessageLimitWhenBacklogSizeIsFive() {
        TeamsEventNotificationConfigV2 config = TeamsEventNotificationConfigV2.builder()
                .backlogSize(5)
                .build();

        // Global setting is at 50 and the message override is 5 then the backlog size = 5
        List<MessageSummary> messageSummaries = teamsEventNotification.getMessageBacklog(eventNotificationContext, config);
        assertThat(messageSummaries.size()).isEqualTo(5);
    }

    @Test
    public void testBacklogMessageLimitWhenBacklogSizeIsZero() {
        TeamsEventNotificationConfigV2 config = TeamsEventNotificationConfigV2.builder()
                .backlogSize(0)
                .build();

        // Global setting is at 50 and the message override is 0 then the backlog size = 50
        List<MessageSummary> messageSummaries = teamsEventNotification.getMessageBacklog(eventNotificationContext, config);
        assertThat(messageSummaries.size()).isEqualTo(50);
    }

    @Test
    public void testBacklogMessageLimitWhenEventNotificationContextIsNull() {
        TeamsEventNotificationConfigV2 config = TeamsEventNotificationConfigV2.builder()
                .backlogSize(0)
                .build();

        // Global setting is at 50 and the eventNotificationContext is null then the message summaries is null
        List<MessageSummary> messageSummaries = teamsEventNotification.getMessageBacklog(null, config);
        assertThat(messageSummaries).isNull();
    }

    private void getDummyTeamsNotificationConfig() {
        notificationConfig = TeamsEventNotificationConfigV2.builder()
                .type(TeamsEventNotificationConfigV2.TYPE_NAME)
                .webhookUrl("https://prod-123.azure.com/webhook/abdef")
                .backlogSize(1)
                .build();
    }

    private NotificationDto getNotificationDto() {
        return NotificationDto.builder()
                .title("Foobar")
                .id("1234")
                .description("")
                .config(notificationConfig)
                .build();
    }

    private EventNotificationContext getEventNotificationContextToSimulateNullPointerException() {
        final DateTime now = DateTime.parse("2019-01-01T00:00:00.000Z");
        final ImmutableList<String> keyTuple = ImmutableList.of("a", "b");

        final EventDto eventDto = EventDto.builder()
                .id("01DF119QKMPCR5VWBXS8783799")
                .eventDefinitionType("aggregation-v1")
                .eventDefinitionId("54e3deadbeefdeadbeefaffe")
                .originContext("urn:graylog:message:es:graylog_0:199a616d-4d48-4155-b4fc-339b1c3129b2")
                .eventTimestamp(now)
                .processingTimestamp(now)
                .streams(ImmutableSet.of("000000000000000000000002"))
                .sourceStreams(ImmutableSet.of("000000000000000000000001"))
                .message("Test message")
                .source("source")
                .keyTuple(keyTuple)
                .key(String.join("|", keyTuple))
                .priority(4)
                .alert(false)
                .fields(ImmutableMap.of("hello", "world"))
                .build();

        // Uses the eventDefinitionDto from NotificationTestData.getDummyContext in the setup method
        EventDefinitionDto eventDefinitionDto = eventNotificationContext.eventDefinition().orElseThrow(NullPointerException::new);
        return EventNotificationContext.builder()
                .notificationId("1234")
                .notificationConfig(notificationConfig)
                .event(eventDto)
                .eventDefinition(eventDefinitionDto)
                .build();
    }

    private void givenGoodNotificationService() {
        given(mockNotificationService.buildNow()).willReturn(new NotificationImpl().addTimestamp(Tools.nowUTC()));
    }

    private void givenTeamsClientThrowsPermException() throws TemporaryEventNotificationException, PermanentEventNotificationException {
        doThrow(PermanentEventNotificationException.class)
                .when(mockrequestClient)
                .send(any(), anyString());
    }

    private ImmutableList<MessageSummary> generateMessageSummaries(int size) {
        List<MessageSummary> messageSummaries = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            MessageSummary summary = new MessageSummary("graylog_" + i, messageFactory.createMessage("Test message_" + i + " : with a colon and another colon : just for good measure", "source" + i, new DateTime(2020, 9, 6, 17, 0, DateTimeZone.UTC)));
            messageSummaries.add(summary);
        }
        return ImmutableList.copyOf(messageSummaries);
    }

    private TeamsEventNotificationConfigV2 buildInvalidTemplate() {
        TeamsEventNotificationConfigV2.Builder builder = TeamsEventNotificationConfigV2.builder();
        builder.adaptiveCard("{${if backlog}\"invalid_json\": true }");
        return builder.build();
    }
}
