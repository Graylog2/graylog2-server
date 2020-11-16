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
package org.graylog2.alerts;

import com.floreysoft.jmte.Engine;
import org.graylog2.configuration.EmailConfiguration;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.net.URI;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FormattedEmailAlertSenderTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private NotificationService mockNotificationService;
    @Mock
    private NodeId mockNodeId;

    private FormattedEmailAlertSender emailAlertSender;
    private final Engine templateEngine = new Engine();

    @Before
    public void setUp() throws Exception {
        this.emailAlertSender = new FormattedEmailAlertSender(new EmailConfiguration(), mockNotificationService, mockNodeId, templateEngine);
    }

    @Test
    public void buildSubjectUsesCustomSubject() throws Exception {
        Configuration pluginConfig = new Configuration(Collections.<String, Object>singletonMap("subject", "Test"));
        emailAlertSender.initialize(pluginConfig);

        Stream stream = mock(Stream.class);
        AlertCondition.CheckResult checkResult = mock(AbstractAlertCondition.CheckResult.class);

        String subject = emailAlertSender.buildSubject(stream, checkResult, Collections.<Message>emptyList());

        assertThat(subject).isEqualTo("Test");
    }

    @Test
    public void buildSubjectUsesDefaultSubjectIfConfigDoesNotExist() throws Exception {
        Stream stream = mock(Stream.class);
        when(stream.getTitle()).thenReturn("Stream Title");

        AlertCondition.CheckResult checkResult = mock(AbstractAlertCondition.CheckResult.class);
        when(checkResult.getResultDescription()).thenReturn("This is the alert description.");
        String subject = emailAlertSender.buildSubject(stream, checkResult, Collections.<Message>emptyList());

        assertThat(subject).isEqualTo("Graylog alert for stream: Stream Title: This is the alert description.");
    }

    @Test
    public void buildBodyUsesCustomBody() throws Exception {
        Configuration pluginConfig = new Configuration(Collections.<String, Object>singletonMap("body", "Test: ${stream.id}"));
        emailAlertSender.initialize(pluginConfig);

        Stream stream = mock(Stream.class);
        when(stream.getId()).thenReturn("123456");

        AlertCondition.CheckResult checkResult = mock(AbstractAlertCondition.CheckResult.class);

        String body = emailAlertSender.buildBody(stream, checkResult, Collections.<Message>emptyList());

        assertThat(body).isEqualTo("Test: 123456");
    }

    @Test
    public void buildBodyUsesDefaultBodyIfConfigDoesNotExist() throws Exception {
        Stream stream = mock(Stream.class);
        when(stream.getId()).thenReturn("123456");
        when(stream.getTitle()).thenReturn("Stream Title");

        AlertCondition alertCondition = mock(AlertCondition.class);

        AlertCondition.CheckResult checkResult = mock(AbstractAlertCondition.CheckResult.class);
        when(checkResult.getTriggeredAt()).thenReturn(new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC));
        when(checkResult.getTriggeredCondition()).thenReturn(alertCondition);

        String body = emailAlertSender.buildBody(stream, checkResult, Collections.<Message>emptyList());

        assertThat(body).containsSequence(
                "Date: 2015-01-01T00:00:00.000Z\n",
                "Stream ID: 123456\n",
                "Stream title: Stream Title"
        );
    }

    @Test
    public void buildBodyContainsURLIfWebInterfaceURLIsSet() throws Exception {
        final EmailConfiguration configuration = new EmailConfiguration() {
            @Override
            public URI getWebInterfaceUri() {
                return URI.create("https://localhost");
            }
        };
        this.emailAlertSender = new FormattedEmailAlertSender(configuration, mockNotificationService, mockNodeId, templateEngine);

        Stream stream = mock(Stream.class);
        when(stream.getId()).thenReturn("123456");
        when(stream.getTitle()).thenReturn("Stream Title");

        AlertCondition alertCondition = mock(AlertCondition.class);

        AlertCondition.CheckResult checkResult = mock(AbstractAlertCondition.CheckResult.class);
        when(checkResult.getTriggeredAt()).thenReturn(new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC));
        when(checkResult.getTriggeredCondition()).thenReturn(alertCondition);

        String body = emailAlertSender.buildBody(stream, checkResult, Collections.<Message>emptyList());

        assertThat(body).contains("Stream URL: https://localhost/streams/123456/");
    }

    @Test
    public void buildBodyContainsInfoMessageIfWebInterfaceURLIsNotSet() throws Exception {
        final EmailConfiguration configuration = new EmailConfiguration() {
            @Override
            public URI getWebInterfaceUri() {
                return null;
            }
        };
        this.emailAlertSender = new FormattedEmailAlertSender(configuration, mockNotificationService, mockNodeId, templateEngine);

        Stream stream = mock(Stream.class);
        when(stream.getId()).thenReturn("123456");
        when(stream.getTitle()).thenReturn("Stream Title");

        AlertCondition alertCondition = mock(AlertCondition.class);

        AlertCondition.CheckResult checkResult = mock(AbstractAlertCondition.CheckResult.class);
        when(checkResult.getTriggeredAt()).thenReturn(new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC));
        when(checkResult.getTriggeredCondition()).thenReturn(alertCondition);

        String body = emailAlertSender.buildBody(stream, checkResult, Collections.<Message>emptyList());

        assertThat(body).contains("Stream URL: Please configure 'transport_email_web_interface_url' in your Graylog configuration file.");
    }

    @Test
    public void buildBodyContainsInfoMessageIfWebInterfaceURLIsIncomplete() throws Exception {
        final EmailConfiguration configuration = new EmailConfiguration() {
            @Override
            public URI getWebInterfaceUri() {
                return URI.create("");
            }
        };
        this.emailAlertSender = new FormattedEmailAlertSender(configuration, mockNotificationService, mockNodeId, templateEngine);

        Stream stream = mock(Stream.class);
        when(stream.getId()).thenReturn("123456");
        when(stream.getTitle()).thenReturn("Stream Title");

        AlertCondition alertCondition = mock(AlertCondition.class);

        AlertCondition.CheckResult checkResult = mock(AbstractAlertCondition.CheckResult.class);
        when(checkResult.getTriggeredAt()).thenReturn(new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC));
        when(checkResult.getTriggeredCondition()).thenReturn(alertCondition);

        String body = emailAlertSender.buildBody(stream, checkResult, Collections.<Message>emptyList());

        assertThat(body).contains("Stream URL: Please configure 'transport_email_web_interface_url' in your Graylog configuration file.");
    }

    @Test
    public void defaultBodyTemplateDoesNotShowBacklogIfBacklogIsEmpty() throws Exception {
        FormattedEmailAlertSender emailAlertSender = new FormattedEmailAlertSender(new EmailConfiguration(), mockNotificationService, mockNodeId, templateEngine);

        Stream stream = mock(Stream.class);
        when(stream.getId()).thenReturn("123456");
        when(stream.getTitle()).thenReturn("Stream Title");

        AlertCondition alertCondition = mock(AlertCondition.class);

        AlertCondition.CheckResult checkResult = mock(AbstractAlertCondition.CheckResult.class);
        when(checkResult.getTriggeredAt()).thenReturn(new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC));
        when(checkResult.getTriggeredCondition()).thenReturn(alertCondition);

        String body = emailAlertSender.buildBody(stream, checkResult, Collections.<Message>emptyList());

        assertThat(body)
                .contains("<No backlog>\n")
                .doesNotContain("Last messages accounting for this alert:\n");
    }

    @Test
    public void defaultBodyTemplateShowsBacklogIfBacklogIsNotEmpty() throws Exception {
        FormattedEmailAlertSender emailAlertSender = new FormattedEmailAlertSender(new EmailConfiguration(), mockNotificationService, mockNodeId, templateEngine);

        Stream stream = mock(Stream.class);
        when(stream.getId()).thenReturn("123456");
        when(stream.getTitle()).thenReturn("Stream Title");

        AlertCondition alertCondition = mock(AlertCondition.class);

        AlertCondition.CheckResult checkResult = mock(AbstractAlertCondition.CheckResult.class);
        when(checkResult.getTriggeredAt()).thenReturn(new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC));
        when(checkResult.getTriggeredCondition()).thenReturn(alertCondition);

        Message message = new Message("Test", "source", new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC));
        String body = emailAlertSender.buildBody(stream, checkResult, Collections.singletonList(message));

        assertThat(body)
                .doesNotContain("<No backlog>\n")
                .containsSequence(
                        "Last messages accounting for this alert:\n",
                        message.toString());
    }
}
