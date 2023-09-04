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
import org.graylog.events.notifications.types.HTTPEventNotificationConfig;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.integrations.notifications.types.util.RequestClient;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.notifications.NotificationImpl;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
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
public class TeamsEventNotificationTest {

    //code under test
    TeamsEventNotification teamsEventNotification;

    private final NodeId nodeId = new SimpleNodeId("12345");

    @Mock
    NotificationService mockNotificationService;

    @Mock
    RequestClient mockrequestClient;

    @Mock
    EventNotificationService notificationCallbackService;

    private TeamsEventNotificationConfig teamsEventNotificationConfig;
    private EventNotificationContext eventNotificationContext;

    private final String expectedColor = "#FF2052";
    private final String expectedImage = "iconUrl";

    @Before
    public void setUp() {

        getDummyTeamsNotificationConfig();
        eventNotificationContext = NotificationTestData.getDummyContext(getHttpNotification(), "ayirp").toBuilder().notificationConfig(teamsEventNotificationConfig).build();
        final ImmutableList<MessageSummary> messageSummaries = generateMessageSummaries(50);
        when(notificationCallbackService.getBacklogForEvent(eventNotificationContext)).thenReturn(messageSummaries);

        teamsEventNotification = new TeamsEventNotification(notificationCallbackService,
                new ObjectMapperProvider(),
                Engine.createEngine(),
                mockNotificationService,
                nodeId,
                mockrequestClient,
                new HttpConfiguration());
    }

    private void getDummyTeamsNotificationConfig() {
        teamsEventNotificationConfig = TeamsEventNotificationConfig.builder()
                .type(TeamsEventNotificationConfig.TYPE_NAME)
                .color(expectedColor)
                .webhookUrl("axzzzz")
                .backlogSize(1)
                .iconUrl(expectedImage)
                .customMessage("a custom message")
                .build();
    }

    private TeamsEventNotificationConfig getTemplatedTimestampConfig() {
        return TeamsEventNotificationConfig.builder()
                .type(TeamsEventNotificationConfig.TYPE_NAME)
                .color(expectedColor)
                .webhookUrl("axzzzz")
                .backlogSize(1)
                .iconUrl(expectedImage)
                .customMessage("Timestamp: ${event.timestamp}")
                .build();
    }

    private NotificationDto getHttpNotification() {
        return NotificationDto.builder()
                .title("Foobar")
                .id("1234")
                .description("")
                .config(HTTPEventNotificationConfig.Builder.create()
                        .url("http://localhost")
                        .build())
                .build();
    }

    @Test
    public void createTeamsMessage() throws EventNotificationException {
        String expectedText = "**Alert Event Definition Test Title triggered:**\n";
        String expectedSubtitle = "_Event Definition Test Description_";
        TeamsMessage actual = teamsEventNotification.createTeamsMessage(eventNotificationContext, teamsEventNotificationConfig);
        assertThat(actual.type()).isEqualTo(TeamsMessage.VALUE_TYPE);
        assertThat(actual.context()).isEqualTo(TeamsMessage.VALUE_CONTEXT);
        assertThat(actual.color()).isEqualTo(expectedColor);
        assertThat(actual.text()).isEqualTo(expectedText);
        assertThat(actual.sections().size()).isEqualTo(1);
        TeamsMessage.Sections section = actual.sections().iterator().next();
        assertThat(section.activitySubtitle()).isEqualTo(expectedSubtitle);
        assertThat(section.activityImage()).isEqualTo(expectedImage);
        assertThat(section.text().contains("a custom message")).isTrue();
    }

    @After
    public void tearDown() {
        teamsEventNotification = null;
        teamsEventNotificationConfig = null;
        eventNotificationContext = null;
    }

    @Test
    public void buildDefaultMessage() {
        String message = teamsEventNotification.buildDefaultMessage(eventNotificationContext);
        assertThat(message).isNotEmpty();
    }

    @Test
    public void getCustomMessageModel() {
        List<MessageSummary> messageSummaries = generateMessageSummaries(50);
        Map<String, Object> customMessageModel = teamsEventNotification.getCustomMessageModel(eventNotificationContext, teamsEventNotificationConfig.type(), messageSummaries, DateTimeZone.UTC);
        //there are 9 keys and two asserts needs to be implemented (backlog,event)
        assertThat(customMessageModel).isNotNull();
        assertThat(customMessageModel.get("event_definition_description")).isEqualTo("Event Definition Test Description");
        assertThat(customMessageModel.get("event_definition_title")).isEqualTo("Event Definition Test Title");
        assertThat(customMessageModel.get("event_definition_type")).isEqualTo("test-dummy-v1");
        assertThat(customMessageModel.get("type")).isEqualTo("teams-notification-v1");
        assertThat(customMessageModel.get("job_definition_id")).isEqualTo("<unknown>");
        assertThat(customMessageModel.get("job_trigger_id")).isEqualTo("<unknown>");
    }


    @Test(expected = EventNotificationException.class)
    public void executeWithInvalidWebhookUrl() throws EventNotificationException {
        givenGoodNotificationService();
        givenTeamsClientThrowsPermException();
        //when execute is called with a invalid webhook URL, we expect a event notification exception
        teamsEventNotification.execute(eventNotificationContext);
    }


    @Test(expected = EventNotificationException.class)
    public void executeWithNullEventTimerange() throws EventNotificationException {
        EventNotificationContext yetAnotherContext = getEventNotificationContextToSimulateNullPointerException();
        assertThat(yetAnotherContext.event().timerangeStart().isPresent()).isFalse();
        assertThat(yetAnotherContext.event().timerangeEnd().isPresent()).isFalse();
        assertThat(yetAnotherContext.notificationConfig().type()).isEqualTo(TeamsEventNotificationConfig.TYPE_NAME);
        teamsEventNotification.execute(yetAnotherContext);
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

        //uses the eventDefinitionDto from NotificationTestData.getDummyContext in the setup method
        EventDefinitionDto eventDefinitionDto = eventNotificationContext.eventDefinition().orElseThrow(NullPointerException::new);
        return EventNotificationContext.builder()
                .notificationId("1234")
                .notificationConfig(teamsEventNotificationConfig)
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

    @Test
    public void buildCustomMessage() throws PermanentEventNotificationException {
        String expectedCustomMessage = teamsEventNotification.buildCustomMessage(eventNotificationContext, teamsEventNotificationConfig, "test");
        assertThat(expectedCustomMessage).isNotEmpty();

    }

    @Test(expected = PermanentEventNotificationException.class)
    public void buildCustomMessageWithInvalidTemplate() throws EventNotificationException {
        teamsEventNotificationConfig = buildInvalidTemplate();
        teamsEventNotification.buildCustomMessage(eventNotificationContext, teamsEventNotificationConfig, "Title:       ${does't exist}");
    }


    @Test
    public void testCustomMessage() throws PermanentEventNotificationException {

        TeamsEventNotificationConfig TeamsConfig = TeamsEventNotificationConfig.builder()
                .backlogSize(5)
                .build();
        String message = teamsEventNotification.buildCustomMessage(eventNotificationContext, TeamsConfig, "Title: ${event_definition_title}");
        assertThat(message).isEqualTo("Title: Event Definition Test Title");
    }


    @Test
    public void testBacklogMessageLimitWhenBacklogSizeIsFive() {
        TeamsEventNotificationConfig TeamsConfig = TeamsEventNotificationConfig.builder()
                .backlogSize(5)
                .build();

        //global setting is at N and the message override is 5 then the backlog size = 5
        List<MessageSummary> messageSummaries = teamsEventNotification.getMessageBacklog(eventNotificationContext, TeamsConfig);
        assertThat(messageSummaries.size()).isEqualTo(5);
    }

    @Test
    public void testBacklogMessageLimitWhenBacklogSizeIsZero() {
        TeamsEventNotificationConfig TeamsConfig = TeamsEventNotificationConfig.builder()
                .backlogSize(0)
                .build();

        //global setting is at N and the message override is 0 then the backlog size = 50
        List<MessageSummary> messageSummaries = teamsEventNotification.getMessageBacklog(eventNotificationContext, TeamsConfig);
        assertThat(messageSummaries.size()).isEqualTo(50);
    }

    @Test
    public void testBacklogMessageLimitWhenEventNotificationContextIsNull() {
        TeamsEventNotificationConfig TeamsConfig = TeamsEventNotificationConfig.builder()
                .backlogSize(0)
                .build();

        //global setting is at N and the eventNotificationContext is null then the message summaries is null
        List<MessageSummary> messageSummaries = teamsEventNotification.getMessageBacklog(null, TeamsConfig);
        assertThat(messageSummaries).isNull();
    }


    ImmutableList<MessageSummary> generateMessageSummaries(int size) {

        List<MessageSummary> messageSummaries = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            MessageSummary summary = new MessageSummary("graylog_" + i, new Message("Test message_" + i + " : with a colon and another colon : just for good measure", "source" + i, new DateTime(2020, 9, 6, 17, 0, DateTimeZone.UTC)));
            messageSummaries.add(summary);
        }
        return ImmutableList.copyOf(messageSummaries);
    }

    TeamsEventNotificationConfig buildInvalidTemplate() {
        TeamsEventNotificationConfig.Builder builder = TeamsEventNotificationConfig.builder();
        builder.customMessage("Title");
        return builder.build();
    }
}

