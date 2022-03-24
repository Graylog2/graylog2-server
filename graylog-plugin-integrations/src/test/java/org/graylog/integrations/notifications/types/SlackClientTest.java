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

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.graylog.events.notifications.PermanentEventNotificationException;
import org.graylog.events.notifications.TemporaryEventNotificationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class SlackClientTest {
    private static final String TEST_MESSAGE = "Henry Hühnchen(little chicken)";
    private static final String TEST_MESSAGE_1 = "{\"link_names\":false,\"channel\":null,\"text\":\"Henry Hühnchen(little chicken)\"}";

    @Rule
    public Timeout timout = Timeout.seconds(10);

    private final MockWebServer server = new MockWebServer();
    private final OkHttpClient httpClient = new OkHttpClient();

    @Before
    public void setUp() throws IOException {
        server.start();
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void send_sendsHttpRequestAsExpected_whenInputIsGood() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_MESSAGE_1));

        SlackClient slackClient = new SlackClient(httpClient);
        SlackMessage message = new SlackMessage(TEST_MESSAGE);
        slackClient.send(message, server.url("/").toString());

        final RecordedRequest recordedRequest = server.takeRequest();

        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getBody()).isNotNull();
        assertThat(recordedRequest.getBody().readUtf8()).isEqualTo(TEST_MESSAGE_1);
    }

    @Test(expected = TemporaryEventNotificationException.class)
    public void send_throwsTempNotifException_whenHttpClientThrowsIOException() throws Exception {
        final OkHttpClient httpClient =
                this.httpClient.newBuilder().readTimeout(1, TimeUnit.MILLISECONDS).build();

        SlackClient slackClient = new SlackClient(httpClient);
        SlackMessage message = new SlackMessage(TEST_MESSAGE);
        slackClient.send(message, server.url("/").toString());
    }

    @Test(expected = PermanentEventNotificationException.class)
    public void send_throwsPermNotifException_whenPostReturnsHttp402() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(402).setBody(TEST_MESSAGE_1));

        SlackClient slackClient = new SlackClient(httpClient);
        slackClient.send(new SlackMessage(TEST_MESSAGE), server.url("/").toString());
    }

    @Test
    public void doesNotFollowRedirects() {
        server.enqueue(new MockResponse().setResponseCode(302)
                .setHeader("Location", server.url("/redirected")));
        server.enqueue(new MockResponse().setResponseCode(200));

        SlackClient slackClient = new SlackClient(httpClient);
        assertThatThrownBy(() -> slackClient.send(new SlackMessage(TEST_MESSAGE), server.url("/").toString()))
                .isInstanceOf(PermanentEventNotificationException.class)
                .hasMessageContaining("[2xx] but got [302]");
    }

}
