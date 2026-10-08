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

import com.fasterxml.jackson.databind.ObjectMapper;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import org.graylog.events.notifications.PermanentEventNotificationException;
import org.graylog.events.notifications.TemporaryEventNotificationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Timeout(value = 10, unit = TimeUnit.SECONDS)
public class SlackClientTest {

    private final MockWebServer server = new MockWebServer();
    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() throws IOException {
        server.start();
    }

    @AfterEach
    public void tearDown() throws IOException {
        server.close();
    }

    @Test
    public void sendsHttpRequestAsExpected_whenInputIsGood() throws Exception {
        server.enqueue(new MockResponse(200, Headers.of(), ""));

        SlackClient slackClient = new SlackClient(httpClient, objectMapper);
        slackClient.send(getMessage(), server.url("/").toString());

        final RecordedRequest recordedRequest = server.takeRequest();

        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getBody()).isNotNull();
        assertThat(recordedRequest.getBody().string(StandardCharsets.UTF_8)).isEqualTo(objectMapper.writeValueAsString(getMessage()));
    }

    @Test
    public void sendThrowsTempNotifException_whenHttpClientThrowsIOException() throws Exception {
        assertThrows(TemporaryEventNotificationException.class, () -> {
            final OkHttpClient httpClient =
                    this.httpClient.newBuilder().readTimeout(1, TimeUnit.MILLISECONDS).build();

            SlackClient slackClient = new SlackClient(httpClient, objectMapper);
            slackClient.send(getMessage(), server.url("/").toString());
        });
    }

    @Test
    public void sendThrowsPermNotifException_whenPostReturnsHttp402() throws Exception {
        assertThrows(PermanentEventNotificationException.class, () -> {
            server.enqueue(new MockResponse(402, Headers.of(), ""));

            SlackClient slackClient = new SlackClient(httpClient, objectMapper);
            slackClient.send(getMessage(), server.url("/").toString());
        });
    }

    @Test
    public void doesNotFollowRedirects() {
        server.enqueue(new MockResponse(302, Headers.of("Location", server.url("/redirected").toString()), ""));
        server.enqueue(new MockResponse(200, Headers.of(), ""));

        SlackClient slackClient = new SlackClient(httpClient, objectMapper);
        assertThatThrownBy(() -> slackClient.send(getMessage(), server.url("/").toString()))
                .isInstanceOf(PermanentEventNotificationException.class)
                .hasMessageContaining("[2xx] but got [302]");
    }

    private SlackMessage getMessage() {
        SlackMessage.Attachment attachment = SlackMessage.Attachment.builder()
                .color("#FF000000")
                .text("text")
                .build();

        return SlackMessage.builder()
                .iconEmoji(":smile:")
                .iconUrl("iconUrl")
                .username("username")
                .text("text")
                .channel("#general")
                .linkNames(true)
                .attachments(Collections.singleton(attachment))
                .build();
    }

}
