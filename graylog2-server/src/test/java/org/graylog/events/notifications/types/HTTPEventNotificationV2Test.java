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
package org.graylog.events.notifications.types;

import com.google.common.collect.ImmutableList;
import org.graylog.events.configuration.EventsConfigurationProvider;
import org.graylog.events.notifications.EventNotificationService;
import org.graylog2.bindings.providers.DefaultJmteEngineProvider;
import org.graylog2.bindings.providers.JsonSafeEngineProvider;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.bindings.providers.ParameterizedHttpClientProvider;
import org.graylog2.system.urlallowlist.UrlAllowlistNotificationService;
import org.graylog2.system.urlallowlist.UrlAllowlistService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class HTTPEventNotificationV2Test {
    @Mock
    private EventNotificationService notificationCallbackService;
    @Mock
    private ObjectMapperProvider objectMapperProvider;
    @Mock
    private UrlAllowlistService allowlistService;
    @Mock
    private UrlAllowlistNotificationService urlAllowlistNotificationService;
    @Mock
    private EncryptedValueService encryptedValueService;
    @Mock
    private EventsConfigurationProvider configurationProvider;
    @Mock
    private ParameterizedHttpClientProvider parameterizedHttpClientProvider;
    @Mock
    private NotificationService notificationService;
    @Mock
    private NodeId nodeId;

    private HTTPEventNotificationV2 notification;

    @BeforeEach
    void setUp() {
        notification = new HTTPEventNotificationV2(notificationCallbackService, objectMapperProvider,
                allowlistService, urlAllowlistNotificationService, encryptedValueService, configurationProvider,
                new DefaultJmteEngineProvider().get(), new JsonSafeEngineProvider().get(), notificationService, nodeId,
                parameterizedHttpClientProvider);
    }

    @Test
    public void testEscapedQuotesInBacklog() {
        Map<String, Object> model = Map.of(
                "event_definition_title", "<<Test Event Title>>",
                "backlog", createBacklog(),
                "event", createEvent()
        );
        String bodyTemplate = "${if backlog}{\"backlog\": [${foreach backlog message}{ \"title\": \"Message\", \"value\": \"${message.message}\" }${if last_message}${else},${end}${end}]}${end}";
        String body = notification.transformBody(bodyTemplate, HTTPEventNotificationConfigV2.ContentType.JSON, model);
        assertThat(body).contains("\"value\": \"Message with \\\"Double Quotes\\\"");
    }

    @Test
    public void testEscapedQuotesInEventFields() {
        Map<String, Object> model = Map.of(
                "event_definition_title", "<<Test Event Title>>",
                "backlog", createBacklog(),
                "event", createEvent()
        );
        String bodyTemplate = "{\n" +
                "    \"message\": \"${event.message}\\\\n\\\\n${event.fields}\",\n" +
                "    \"title\": \"${event_definition_title}\"\n" +
                "}";
        String body = notification.transformBody(bodyTemplate, HTTPEventNotificationConfigV2.ContentType.JSON, model);
        assertThat(body).contains("\\\"bad_field\\\"");
    }

    @Test
    public void testEscapedQuotesInList() {
        Map<String, Object> model = Map.of(
                "event_definition_title", "<<Test Event Title>>",
                "backlog", createBacklog(),
                "event", createEvent()
        );
        String bodyTemplate = "{\n" +
                "    \"message\": \"${event.message}\\\\n\\\\n${event.list_field}\",\n" +
                "    \"title\": \"${event_definition_title}\"\n" +
                "}";
        String body = notification.transformBody(bodyTemplate, HTTPEventNotificationConfigV2.ContentType.JSON, model);
        assertThat(body).contains("\\\"list_value1\\\"");
    }

    @Test
    public void testDateFormatting() {
        Map<String, Object> model = Map.of(
                "event_definition_title", "<<Test Event Title>>",
                "backlog", createBacklog(),
                "event", createEvent()
        );

        String bodyTemplate = "formatted date: ${event.event_time;date(yyyy-MM-dd HH:mm:ss)}";
        String body = notification.transformBody(bodyTemplate, HTTPEventNotificationConfigV2.ContentType.PLAIN_TEXT, model);
        assertThat(body).isEqualTo("formatted date: 2025-04-02 15:37:46");

        bodyTemplate = "{\"formatted date\": \"${event.event_time;date(HH:mm:ss)}\"}";
        body = notification.transformBody(bodyTemplate, HTTPEventNotificationConfigV2.ContentType.JSON, model);
        assertThat(body).isEqualTo("{\"formatted date\": \"15:37:46\"}");

        bodyTemplate = "formatted_date=${event.event_time;date(dd-MM-yyyy)}";
        body = notification.transformBody(bodyTemplate, HTTPEventNotificationConfigV2.ContentType.FORM_DATA, model);
        assertThat(body).isEqualTo("formatted_date=02-04-2025");
    }

    private ImmutableList<MessageSummary> createBacklog() {
        Message message = new TestMessageFactory().createMessage("Message with \"Double Quotes\"", "Unit Test", DateTime.now(DateTimeZone.UTC));
        MessageSummary summary = new MessageSummary("index1", message);
        return ImmutableList.of(summary);
    }

    private Map<String, Object> createEvent() {
        final Map<String, Object> event = new HashMap<>();
        final Map<String, Object> fields = Map.of(
                "field1", "\"bad_field\"",
                "field2", "A somehow \"worse\" field!"
        );
        event.put("message", "Event Message & Whatnot");
        event.put("fields", fields);
        event.put("list_field", List.of("\"list_value1\"", "\"list_value2\""));
        event.put("event_time", DateTime.parse("2025-04-02T15:37:46.717Z"));
        return event;
    }

}
