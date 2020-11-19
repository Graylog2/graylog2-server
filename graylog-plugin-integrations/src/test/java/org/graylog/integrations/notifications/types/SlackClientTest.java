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

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.graylog.events.notifications.PermanentEventNotificationException;
import org.graylog.events.notifications.TemporaryEventNotificationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class SlackClientTest {

    private OkHttpClient mockHttpClient;
    private static final String TEST_MESSAGE = "Henry Hühnchen(little chicken)";
    private static final String TEST_MESSAGE_1 = "{\"link_names\":false,\"channel\":null,\"text\":\"Henry Hühnchen(little chicken)\"}";


    @Before
    public void setUp() throws Exception {
        mockHttpClient = getMockHttpClient(200);
    }

    @After
    public void tearDown() {
        mockHttpClient = null;
    }


    @Test
    public void send_sendsHttpRequestAsExpected_whenInputIsGood() throws Exception {
        SlackClient slackClient = new SlackClient(mockHttpClient);
        SlackMessage message = new SlackMessage(TEST_MESSAGE);
        slackClient.send(message, "http://url.com/");

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mockHttpClient, times(1)).newCall(requestCaptor.capture());

        assertThat(requestCaptor.getValue()).isNotNull();
        Request sent = requestCaptor.getValue();
        assertThat(sent.url().toString()).isEqualTo("http://url.com/");
        assertThat(sent.method()).isEqualTo("POST");
        assertThat(sent.body()).isNotNull();
        Buffer buffer = new Buffer();
        sent.body().writeTo(buffer);
        assertThat(buffer.readUtf8()).isEqualTo(TEST_MESSAGE_1);

    }

    @Test(expected = TemporaryEventNotificationException.class)
    public void send_throwsTempNotifException_whenHttpClientThrowsIOException() throws Exception {

        final OkHttpClient okHttpClient = mock(OkHttpClient.class);
        final Call remoteCall = mock(Call.class);
        when(remoteCall.execute()).thenThrow(new IOException("Request timeout"));
        when(okHttpClient.newCall(any())).thenReturn(remoteCall);


        SlackClient slackClient = new SlackClient(okHttpClient);
        SlackMessage message = new SlackMessage(TEST_MESSAGE);
        slackClient.send(message, "http://url.com/");
    }

    @Test(expected = PermanentEventNotificationException.class)
    public void send_throwsPermNotifException_whenPostReturnsHttp402() throws Exception {

        final OkHttpClient okHttpClient = getMockHttpClient(402);
        SlackClient slackClient = new SlackClient(okHttpClient);
        SlackMessage message = new SlackMessage("Henry Hühnchen(little chicken)");
        slackClient.send(message, "http://url.com/");
    }


    private static OkHttpClient getMockHttpClient(int httpCode) throws IOException {
        final OkHttpClient okHttpClient = mock(OkHttpClient.class);

        final Call remoteCall = mock(Call.class);

        final Response response = new Response.Builder()
                .request(new Request.Builder().url("http://url.com/").build())
                .protocol(Protocol.HTTP_1_1)
                .code(httpCode).message("").body(
                        ResponseBody.create(
                                MediaType.parse("application/json"),
                                "{\"key\": \"val\"}"
                        ))
                .build();

        when(remoteCall.execute()).thenReturn(response);
        when(okHttpClient.newCall(any())).thenReturn(remoteCall);

        return okHttpClient;
    }


}
