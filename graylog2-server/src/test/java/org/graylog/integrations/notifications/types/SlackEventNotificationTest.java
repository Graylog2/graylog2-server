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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.charset.StandardCharsets;
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
public class SlackEventNotificationTest {
    //code under test
    SlackEventNotification slackEventNotification;

    private final NodeId nodeId = new SimpleNodeId("12345");

    @Mock
    NotificationService mockNotificationService;

    @Mock
    SlackClient mockSlackClient;

    @Mock
    EventNotificationService notificationCallbackService;

    private SlackEventNotificationConfig slackEventNotificationConfig;
    private EventNotificationContext eventNotificationContext;
    private MessageFactory messageFactory = new TestMessageFactory();

    private final String expectedAttachmentText = "a custom message";
    private final String expectedColor = "#FF2052";
    private final String expectedChannel = "#general";
    private final String expectedEmoji = ":smile:";
    private final String expectedIconUrl = "emoji.com/smile.png";
    private final String expectedUsername = "graylog_user1";

    @Before
    public void setUp() {
        getDummySlackNotificationConfig();
        eventNotificationContext = NotificationTestData.getDummyContext(getHttpNotification(), "ayirp").toBuilder().notificationConfig(slackEventNotificationConfig).build();
        final ImmutableList<MessageSummary> messageSummaries = generateMessageSummaries(50);
        when(notificationCallbackService.getBacklogForEvent(eventNotificationContext)).thenReturn(messageSummaries);

        slackEventNotification = new SlackEventNotification(notificationCallbackService, new ObjectMapperProvider(),
                Engine.createEngine(),
                mockNotificationService,
                nodeId,
                mockSlackClient,
                new HttpConfiguration());

    }

    private void getDummySlackNotificationConfig() {
        slackEventNotificationConfig = SlackEventNotificationConfig.builder()
                .notifyChannel(true)
                .type(SlackEventNotificationConfig.TYPE_NAME)
                .color(expectedColor)
                .webhookUrl("axzzzz")
                .channel(expectedChannel)
                .backlogSize(1)
                .customMessage(expectedAttachmentText)
                .linkNames(true)
                .iconEmoji(expectedEmoji)
                .iconUrl(expectedIconUrl)
                .userName(expectedUsername)
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
    public void createSlackMessage() throws EventNotificationException {
        String expectedText = "@channel *Alert _Event Definition Test Title_* triggered:\n> Event Definition Test Description \n";
        SlackMessage actual = slackEventNotification.createSlackMessage(eventNotificationContext, slackEventNotificationConfig);
        assertThat(actual.linkNames()).isTrue();
        assertThat(actual.channel()).isEqualTo(expectedChannel);
        assertThat(actual.text()).isEqualTo(expectedText);
        assertThat(actual.iconUrl()).isEqualTo(expectedIconUrl);
        assertThat(actual.iconEmoji()).isEqualTo(expectedEmoji);
        assertThat(actual.username()).isEqualTo(expectedUsername);
        assertThat(actual.attachments().size()).isEqualTo(1);
        SlackMessage.Attachment attachment = actual.attachments().iterator().next();
        assertThat(attachment.color()).isEqualTo(expectedColor);
        assertThat(attachment.text()).isEqualTo(expectedAttachmentText);
    }

    @After
    public void tearDown() {
        slackEventNotification = null;
        slackEventNotificationConfig = null;
        eventNotificationContext = null;
    }

    @Test
    public void buildDefaultMessage() {
        String message = slackEventNotification.buildDefaultMessage(eventNotificationContext, slackEventNotificationConfig);
        assertThat(message).isNotBlank();
        assertThat(message).isNotEmpty();
        assertThat(message).isNotNull();
        assertThat(message).contains("@channel");
        assertThat(message.getBytes(StandardCharsets.UTF_8).length).isEqualTo(95);
    }

    @Test
    public void getCustomMessageModel() {
        List<MessageSummary> messageSummaries = generateMessageSummaries(50);
        Map<String, Object> customMessageModel = slackEventNotification.getCustomMessageModel(eventNotificationContext, slackEventNotificationConfig.type(), messageSummaries, DateTimeZone.UTC);
        //there are 9 keys and two asserts needs to be implemented (backlog,event)
        assertThat(customMessageModel).isNotNull();
        assertThat(customMessageModel.get("event_definition_description")).isEqualTo("Event Definition Test Description");
        assertThat(customMessageModel.get("event_definition_title")).isEqualTo("Event Definition Test Title");
        assertThat(customMessageModel.get("event_definition_type")).isEqualTo("test-dummy-v1");
        assertThat(customMessageModel.get("type")).isEqualTo("slack-notification-v1");
        assertThat(customMessageModel.get("job_definition_id")).isEqualTo("<unknown>");
        assertThat(customMessageModel.get("job_trigger_id")).isEqualTo("<unknown>");
    }

    @Test(expected = EventNotificationException.class)
    public void executeWithInvalidWebhookUrl() throws EventNotificationException, JsonProcessingException {
        givenGoodNotificationService();
        givenSlackClientThrowsPermException();
        //when execute is called with a invalid webhook URL, we expect a event notification exception
        slackEventNotification.execute(eventNotificationContext);
    }

    @Test(expected = EventNotificationException.class)
    public void executeWithNullEventTimerange() throws EventNotificationException {
        EventNotificationContext yetAnotherContext = getEventNotificationContextToSimulateNullPointerException();
        assertThat(yetAnotherContext.event().timerangeStart().isPresent()).isFalse();
        assertThat(yetAnotherContext.event().timerangeEnd().isPresent()).isFalse();
        assertThat(yetAnotherContext.notificationConfig().type()).isEqualTo(SlackEventNotificationConfig.TYPE_NAME);
        slackEventNotification.execute(yetAnotherContext);
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

        //uses the eventDEfinitionDto from NotificationTestData.getDummyContext in the setup method
        EventDefinitionDto eventDefinitionDto = eventNotificationContext.eventDefinition().get();
        return EventNotificationContext.builder()
                .notificationId("1234")
                .notificationConfig(slackEventNotificationConfig)
                .event(eventDto)
                .eventDefinition(eventDefinitionDto)
                .build();
    }


    private void givenGoodNotificationService() {
        given(mockNotificationService.buildNow()).willReturn(new NotificationImpl().addTimestamp(Tools.nowUTC()));
    }

    private void givenSlackClientThrowsPermException() throws TemporaryEventNotificationException, PermanentEventNotificationException, JsonProcessingException {
        doThrow(PermanentEventNotificationException.class)
                .when(mockSlackClient)
                .send(any(), anyString());

    }

    @Test
    public void buildCustomMessage() throws PermanentEventNotificationException {
        String s = slackEventNotification.buildCustomMessage(eventNotificationContext, slackEventNotificationConfig, "${thisDoesnotExist}");
        assertThat(s).isEmpty();
        String expectedCustomMessage = slackEventNotification.buildCustomMessage(eventNotificationContext, slackEventNotificationConfig, "test");
        assertThat(expectedCustomMessage).isNotEmpty();

    }

    @Test(expected = PermanentEventNotificationException.class)
    public void buildCustomMessageWithInvalidTemplate() throws EventNotificationException {
        slackEventNotificationConfig = buildInvalidTemplate();
        slackEventNotification.buildCustomMessage(eventNotificationContext, slackEventNotificationConfig, "Title:       ${does't exist}");
    }

    @Test
    public void testCustomMessage() throws PermanentEventNotificationException {

        SlackEventNotificationConfig slackConfig = SlackEventNotificationConfig.builder()
                .backlogSize(5)
                .build();
        String message = slackEventNotification.buildCustomMessage(eventNotificationContext, slackConfig, "Ich spreche Deutsch");
        assertThat(message).isEqualTo("Ich spreche Deutsch");
    }

    @Test
    public void testBacklogMessageLimitWhenBacklogSizeIsFive() {
        SlackEventNotificationConfig slackConfig = SlackEventNotificationConfig.builder()
                .backlogSize(5)
                .build();

        //global setting is at N and the message override is 5 then the backlog size = 5
        List<MessageSummary> messageSummaries = slackEventNotification.getMessageBacklog(eventNotificationContext, slackConfig);
        assertThat(messageSummaries.size()).isEqualTo(5);
    }

    @Test
    public void testBacklogMessageLimitWhenBacklogSizeIsZero() {
        SlackEventNotificationConfig slackConfig = SlackEventNotificationConfig.builder()
                .backlogSize(0)
                .build();

        //global setting is at N and the message override is 0 then the backlog size = 50
        List<MessageSummary> messageSummaries = slackEventNotification.getMessageBacklog(eventNotificationContext, slackConfig);
        assertThat(messageSummaries.size()).isEqualTo(50);
    }

    @Test
    public void testBacklogMessageLimitWhenEventNotificationContextIsNull() {
        SlackEventNotificationConfig slackConfig = SlackEventNotificationConfig.builder()
                .backlogSize(0)
                .build();

        //global setting is at N and the eventNotificationContext is null then the message summaries is null
        List<MessageSummary> messageSummaries = slackEventNotification.getMessageBacklog(null, slackConfig);
        assertThat(messageSummaries).isNull();
    }

    @Test
    public void testChannelAlertWithNoTitle() throws EventNotificationException {
        SlackEventNotificationConfig slackConfig = SlackEventNotificationConfig.builder()
                .includeTitle(false)
                .notifyChannel(true)
                .customMessage("A custom message")
                .iconEmoji("")
                .iconUrl("")
                .userName("")
                .build();

        SlackMessage message = slackEventNotification.createSlackMessage(eventNotificationContext, slackConfig);
        assertThat(message.attachments().iterator().next().text()).startsWith("@channel");
        assertThat(message.text()).isNull();
    }

    @Test
    public void testChannelAlertWithTitle() throws EventNotificationException {
        SlackEventNotificationConfig slackConfig = SlackEventNotificationConfig.builder()
                .includeTitle(true)
                .notifyChannel(true)
                .customMessage("A custom message")
                .iconEmoji("")
                .iconUrl("")
                .userName("")
                .build();

        SlackMessage message = slackEventNotification.createSlackMessage(eventNotificationContext, slackConfig);
        assertThat(message.attachments().iterator().next().text()).doesNotStartWith("@channel");
        assertThat(message.text()).startsWith("@channel");
    }

    ImmutableList<MessageSummary> generateMessageSummaries(int size) {
        List<MessageSummary> messageSummaries = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            MessageSummary summary = new MessageSummary("graylog_" + i, messageFactory.createMessage("Test message_" + i, "source" + i, new DateTime(2020, 9, 6, 17, 0, DateTimeZone.UTC)));
            messageSummaries.add(summary);
        }
        return ImmutableList.copyOf(messageSummaries);
    }

    SlackEventNotificationConfig buildInvalidTemplate() {
        SlackEventNotificationConfig.Builder builder = new AutoValue_SlackEventNotificationConfig.Builder().create();
        builder.customMessage("Title");
        return builder.build();
    }
}
