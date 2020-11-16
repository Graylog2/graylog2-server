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
package org.graylog2.alarmcallbacks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.graylog2.alerts.AbstractAlertCondition;
import org.graylog2.alerts.types.DummyAlertCondition;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackException;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.streams.StreamMock;
import org.graylog2.system.urlwhitelist.UrlWhitelistService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HTTPAlarmCallbackTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private OkHttpClient httpClient;
    private ObjectMapper objectMapper;
    private HTTPAlarmCallback alarmCallback;
    private UrlWhitelistService whitelistService;

    private MockWebServer server;

    @Before
    public void setUp() throws Exception {
        httpClient = new OkHttpClient();
        objectMapper = new ObjectMapperProvider().get();
        whitelistService = mock(UrlWhitelistService.class);
        alarmCallback = new HTTPAlarmCallback(httpClient, objectMapper, whitelistService);

        server = new MockWebServer();
    }

    @After
    public void shutDown() throws IOException {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void initializeStoresConfiguration() throws Exception {
        final Map<String, Object> configMap = ImmutableMap.of("url", "http://example.com/");
        final Configuration configuration = new Configuration(configMap);
        alarmCallback.initialize(configuration);

        assertThat(alarmCallback.getAttributes()).isEqualTo(configMap);
    }

    @Test
    public void callSucceedsIfRemoteRequestSucceeds() throws Exception {
        when(whitelistService.isWhitelisted(anyString())).thenReturn(true);

        server.enqueue(new MockResponse().setResponseCode(200));
        server.start();

        final Configuration configuration = new Configuration(ImmutableMap.of("url", server.url("/").toString()));
        alarmCallback.initialize(configuration);
        alarmCallback.checkConfiguration();

        final Stream stream = new StreamMock(
                ImmutableMap.of(
                        "_id", "stream-id",
                        "title", "Stream Title",
                        "description", "Stream Description"),
                ImmutableList.of()
        );
        final AlertCondition alertCondition = new DummyAlertCondition(
                stream,
                "condition-id",
                new DateTime(2016, 9, 6, 17, 0, DateTimeZone.UTC),
                "user",
                ImmutableMap.of(),
                "Alert Condition Title"
        );
        final List<MessageSummary> messageSummaries = ImmutableList.of(
                new MessageSummary("graylog_1", new Message("Test message 1", "source1", new DateTime(2016, 9, 6, 17, 0, DateTimeZone.UTC))),
                new MessageSummary("graylog_2", new Message("Test message 2", "source2", new DateTime(2016, 9, 6, 17, 0, DateTimeZone.UTC)))
        );
        final AlertCondition.CheckResult checkResult = new AbstractAlertCondition.CheckResult(
                true,
                alertCondition,
                "Result Description",
                new DateTime(2016, 9, 6, 17, 0, DateTimeZone.UTC),
                messageSummaries
        );

        alarmCallback.call(stream, checkResult);

        final RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo("/");
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/json");
        assertThat(request.getBodySize()).isPositive();

        final String requestBody = request.getBody().readUtf8();
        final JsonNode jsonNode = objectMapper.readTree(requestBody);
        assertThat(jsonNode.get("check_result").get("matching_messages").size()).isEqualTo(2);
        assertThat(jsonNode.get("check_result").get("triggered").asBoolean()).isTrue();
        assertThat(jsonNode.get("check_result").get("triggered_at").asText()).isEqualTo("2016-09-06T17:00:00.000Z");
        assertThat(jsonNode.get("stream").get("id").asText()).isEqualTo("stream-id");
    }

    @Test
    public void callThrowsAlarmCallbackExceptionIfRemoteServerReturnsError() throws Exception {
        when(whitelistService.isWhitelisted(anyString())).thenReturn(true);

        server.enqueue(new MockResponse().setResponseCode(500));
        server.start();

        final Configuration configuration = new Configuration(ImmutableMap.of("url", server.url("/").toString()));
        alarmCallback.initialize(configuration);
        alarmCallback.checkConfiguration();

        final Stream stream = new StreamMock(Collections.singletonMap("_id", "stream-id"));
        final AlertCondition alertCondition = new DummyAlertCondition(
                stream,
                "alert-id",
                new DateTime(2017, 3, 29, 12, 0, DateTimeZone.UTC),
                "user",
                Collections.emptyMap(),
                "title"
        );
        final AlertCondition.CheckResult checkResult = new AbstractAlertCondition.CheckResult(
                true,
                alertCondition,
                "Result Description",
                new DateTime(2016, 9, 6, 17, 0, DateTimeZone.UTC),
                Collections.emptyList()
        );

        expectedException.expect(AlarmCallbackException.class);
        expectedException.expectMessage("Expected successful HTTP response [2xx] but got [500].");

        alarmCallback.call(stream, checkResult);

        final RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo("/");
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/json");
        assertThat(request.getBodySize()).isPositive();
    }

    @Test
    public void callThrowsAlarmCallbackExceptionIfURLIsMalformed() throws Exception {
        final Configuration configuration = new Configuration(ImmutableMap.of("url", "!FOOBAR"));
        alarmCallback.initialize(configuration);

        final Stream stream = new StreamMock(Collections.singletonMap("_id", "stream-id"));
        final AlertCondition alertCondition = new DummyAlertCondition(
                stream,
                "alert-id",
                new DateTime(2017, 3, 29, 12, 0, DateTimeZone.UTC),
                "user",
                Collections.emptyMap(),
                "title"
        );
        final AlertCondition.CheckResult checkResult = new AbstractAlertCondition.CheckResult(
                true,
                alertCondition,
                "Result Description",
                new DateTime(2016, 9, 6, 17, 0, DateTimeZone.UTC),
                Collections.emptyList()
        );

        expectedException.expect(AlarmCallbackException.class);
        expectedException.expectMessage("Malformed URL: !FOOBAR");

        alarmCallback.call(stream, checkResult);
    }

    @Test
    public void callThrowsAlarmCallbackExceptionIfURLIsNotWhitelisted() throws Exception {
        final Configuration configuration = new Configuration(ImmutableMap.of("url", "http://not-whitelisted"));
        alarmCallback.initialize(configuration);

        final Stream stream = new StreamMock(Collections.singletonMap("_id", "stream-id"));

        expectedException.expect(AlarmCallbackException.class);
        expectedException.expectMessage("URL <http://not-whitelisted> is not whitelisted.");

        alarmCallback.call(stream, null);
    }

    @Test
    public void callThrowsAlarmCallbackExceptionIfRequestBodyCanNotBeBuilt() throws Exception {
        final Configuration configuration = new Configuration(ImmutableMap.of("url", "http://example.org"));
        alarmCallback.initialize(configuration);

        final Stream stream = mock(Stream.class);
        final AlertCondition alertCondition = mock(AlertCondition.class);
        final List<MessageSummary> messageSummaries = ImmutableList.of();
        final AlertCondition.CheckResult checkResult = new AbstractAlertCondition.CheckResult(
                true,
                alertCondition,
                "Result Description",
                new DateTime(2016, 9, 6, 17, 0, DateTimeZone.UTC),
                messageSummaries
        ) {
            @Override
            public String getResultDescription() {
                throw new RuntimeException("Boom");
            }
        };

        expectedException.expect(AlarmCallbackException.class);
        expectedException.expectMessage("Unable to serialize alarm");

        alarmCallback.call(stream, checkResult);
    }

    @Test
    public void getRequestedConfigurationContainsURLField() throws Exception {
        final ConfigurationRequest requestedConfiguration = alarmCallback.getRequestedConfiguration();
        final ConfigurationField field = requestedConfiguration.getField("url");
        assertThat(field).isNotNull();
        assertThat(field.getHumanName()).isEqualTo("URL");
        assertThat(field.isOptional()).isEqualTo(ConfigurationField.Optional.NOT_OPTIONAL);
    }

    @Test
    public void getNameReturnsNameOfHTTPAlarmCallback() throws Exception {
        assertThat(alarmCallback.getName()).isEqualTo("HTTP Alarm Callback [Deprecated]");
    }

    @Test
    public void checkConfigurationSucceedsWithValidConfiguration() throws Exception {
        when(whitelistService.isWhitelisted(anyString())).thenReturn(true);

        final Map<String, Object> configMap = ImmutableMap.of("url", "http://example.com/");
        final Configuration configuration = new Configuration(configMap);
        alarmCallback.initialize(configuration);

        alarmCallback.checkConfiguration();
    }

    @Test
    public void checkConfigurationFailsWithMissingURL() throws Exception {
        final Map<String, Object> configMap = ImmutableMap.of();
        final Configuration configuration = new Configuration(configMap);
        alarmCallback.initialize(configuration);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("URL parameter is missing.");

        alarmCallback.checkConfiguration();
    }

    @Test
    public void checkConfigurationFailsWithEmptyURL() throws Exception {
        final Map<String, Object> configMap = ImmutableMap.of("url", "");
        final Configuration configuration = new Configuration(configMap);
        alarmCallback.initialize(configuration);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("URL parameter is missing.");

        alarmCallback.checkConfiguration();
    }

    @Test
    public void checkConfigurationFailsWithInvalidURL() throws Exception {
        final Map<String, Object> configMap = ImmutableMap.of("url", "!Foobar!");
        final Configuration configuration = new Configuration(configMap);
        alarmCallback.initialize(configuration);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("Malformed URL");

        alarmCallback.checkConfiguration();
    }

}
