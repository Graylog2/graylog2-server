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

import com.floreysoft.jmte.Engine;
import com.google.common.collect.ImmutableList;
import org.graylog.events.notifications.EventNotificationContext;
import org.graylog.events.notifications.EventNotificationException;
import org.graylog.events.notifications.EventNotificationService;
import org.graylog.events.notifications.NotificationDto;
import org.graylog.events.notifications.NotificationTestData;
import org.graylog.events.notifications.PermanentEventNotificationException;
import org.graylog.events.notifications.TemporaryEventNotificationException;
import org.graylog.events.notifications.types.HTTPEventNotificationConfig;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.List;
import java.util.Map;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SlackEventNotificationTest  {

    @Mock
    NodeId mockNodeId;

    @Mock
    NotificationService mockNotificationService;

    @Mock
    SlackClient slackClient;

    @Mock
    EventNotificationService notificationCallbackService;

    private SlackEventNotification slackEventNotification;
    private SlackEventNotificationConfig slackEventNotificationConfig;
    private EventNotificationContext eventNotificationContext;



    @Before
    public void setUp() {

        given_a_bad_event_notificationConfig();
        eventNotificationContext = NotificationTestData.getDummyContext(getHttpNotification(), "ayirp").toBuilder().notificationConfig(slackEventNotificationConfig).build();


        final ImmutableList<MessageSummary> messageSummaries = ImmutableList.of(
                new MessageSummary("graylog_1", new Message("Test message 1", "source1", new DateTime(2020, 9, 6, 17, 0, DateTimeZone.UTC))),
                new MessageSummary("graylog_2", new Message("Test message 2", "source2", new DateTime(2020, 9, 6, 17, 0, DateTimeZone.UTC)))
        );

        when(notificationCallbackService.getBacklogForEvent(eventNotificationContext)).thenReturn(messageSummaries);

        slackEventNotification = new SlackEventNotification(notificationCallbackService, new ObjectMapperProvider().get(),
                                                            Engine.createEngine(),
                                                            mockNotificationService,
                                                            mockNodeId,slackClient);

    }

    private void given_a_bad_event_notificationConfig() {
        slackEventNotificationConfig = new AutoValue_SlackEventNotificationConfig.Builder()
                .notifyChannel(true)
                .type(SlackEventNotificationConfig.TYPE_NAME)
                .color("#FF2052")
                .webhookUrl("axzzzz")
                .channel("#general")
                .customMessage("a custom message")
                .linkNames(true)
                .build();
        slackEventNotificationConfig.validate();
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
       String expected = "{\"link_names\":true,\"attachments\":[{\"fallback\":\"Custom Message\",\"text\":\"a custom message\",\"pretext\":\"Custom Message:\",\"color\":\"#FF2052\"}],\"channel\":\"#general\",\"text\":\"@channel *Alert _Event Definition Test Title_* triggered:\\n> Event Definition Test Description \\n\"}";
       SlackMessage message =  slackEventNotification.createSlackMessage(eventNotificationContext, slackEventNotificationConfig);
       String actual  = message.getJsonString();
       assertThat(actual).isEqualTo(expected);

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
        assertThat(message.getBytes().length).isEqualTo(95);
    }

    @Test
    public void getAlarmBacklog() {
        List<MessageSummary> messageSummaries = slackEventNotification.getAlarmBacklog(eventNotificationContext);
        assertThat(messageSummaries.size()).isEqualTo(2);
    }

    @Test
    public void getCustomMessageModel() {
        List<MessageSummary> messageSummaries = slackEventNotification.getAlarmBacklog(eventNotificationContext);
        Map<String, Object> customMessageModel = slackEventNotification.getCustomMessageModel(eventNotificationContext, slackEventNotificationConfig, messageSummaries);
        //there are 9 keys and two asserts needs to be implemented (backlog,event)
        assertThat(customMessageModel).isNotNull();
        assertThat(customMessageModel.get("event_definition_description")).isEqualTo("Event Definition Test Description");
        assertThat(customMessageModel.get("event_definition_title")).isEqualTo("Event Definition Test Title");
        assertThat(customMessageModel.get("event_definition_type")).isEqualTo("test-dummy-v1");
        assertThat(customMessageModel.get("type")).isEqualTo("slack-notification-v1");
        assertThat(customMessageModel.get("job_definition_id")).isEqualTo("<unknown>");
        assertThat(customMessageModel.get("job_trigger_id")).isEqualTo("<unknown>");
    }


    @Test (expected = PermanentEventNotificationException.class)
    public void execute_with_invalid_webhook_url() throws TemporaryEventNotificationException, PermanentEventNotificationException {
       //has an invalid webhook url
       slackEventNotification.execute(eventNotificationContext);
    }


    @Test
    public void buildCustomMessage() throws PermanentEventNotificationException {
       String s =  slackEventNotification.buildCustomMessage(eventNotificationContext,slackEventNotificationConfig,"${thisDoesnotExist}");
       assertThat(s).isEmpty();
       String expectedCustomMessage =  slackEventNotification.buildCustomMessage(eventNotificationContext,slackEventNotificationConfig,"test");
       assertThat(expectedCustomMessage).isNotEmpty();

    }

    @Test(expected = PermanentEventNotificationException.class)
    public void buildCustomMessage_with_invalidTemplate() throws EventNotificationException {
        slackEventNotificationConfig = buildInvalidTemplate();
        slackEventNotification.buildCustomMessage(eventNotificationContext,slackEventNotificationConfig,"Title:       ${does't exist}");
    }

    SlackEventNotificationConfig buildInvalidTemplate() {
        SlackEventNotificationConfig.Builder builder = new AutoValue_SlackEventNotificationConfig.Builder().create();
        builder.customMessage("Title");
        return builder.build();
    }
}
