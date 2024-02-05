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
package org.graylog2.inputs.transports;

import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okio.Buffer;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.graylog2.inputs.transports.HttpPollTransport.CK_CONTENT_TYPE;
import static org.graylog2.inputs.transports.HttpPollTransport.CK_HTTP_BODY;
import static org.graylog2.inputs.transports.HttpPollTransport.CK_HTTP_METHOD;
import static org.graylog2.inputs.transports.HttpPollTransport.GET;
import static org.graylog2.inputs.transports.HttpPollTransport.POST;
import static org.graylog2.inputs.transports.HttpPollTransport.PUT;
import static org.graylog2.inputs.transports.HttpPollTransport.parseHeaders;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class HttpPollTransportTest {
    @Mock
    private EventBus serverEventBus;
    @Mock
    private ServerStatus serverStatus;
    @Mock
    private ScheduledExecutorService scheduler;
    @Mock
    private OkHttpClient httpClient;
    @Mock
    private EncryptedValueService encryptedValueService;

    @Test
    public void testParseHeaders() throws Exception {
        assertEquals(0, parseHeaders("").size());
        assertEquals(0, parseHeaders(" ").size());
        assertEquals(0, parseHeaders(" . ").size());
        assertEquals(0, parseHeaders("foo").size());
        assertEquals(1, parseHeaders("X-Foo: Bar").size());

        Map<String, String> expectedSingle = ImmutableMap.of("Accept", "application/json");
        Map<String, String> expectedMulti = ImmutableMap.of(
                "Accept", "application/json",
                "X-Foo", "bar");

        assertEquals(expectedMulti, parseHeaders("Accept: application/json, X-Foo: bar"));
        assertEquals(expectedSingle, parseHeaders("Accept: application/json"));

        assertEquals(expectedMulti, parseHeaders(" Accept:  application/json,X-Foo:bar"));
        assertEquals(expectedMulti, parseHeaders("Accept:application/json,   X-Foo: bar "));
        assertEquals(expectedMulti, parseHeaders("Accept:    application/json,     X-Foo: bar"));
        assertEquals(expectedMulti, parseHeaders("Accept :application/json,   X-Foo: bar "));

        assertEquals(expectedSingle, parseHeaders(" Accept: application/json"));
        assertEquals(expectedSingle, parseHeaders("Accept:application/json"));
        assertEquals(expectedSingle, parseHeaders(" Accept: application/json "));
        assertEquals(expectedSingle, parseHeaders(" Accept :application/json "));

    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testGetRequestBuilder() throws IOException, NullPointerException {
        Map<String, Object> configVals = Map.of();
        Configuration config = new Configuration(configVals);
        HttpPollTransport httpPollTransport = new HttpPollTransport(config, serverEventBus, serverStatus, scheduler, httpClient, encryptedValueService);
        Request request = httpPollTransport.getRequestBuilder()
                .url("https://url.com")
                .build();

        assertEquals(GET, request.method());
        assertNull(request.body());

        configVals = Map.of(
                CK_HTTP_METHOD, GET,
                CK_HTTP_BODY, "body",
                CK_CONTENT_TYPE, APPLICATION_JSON);
        config = new Configuration(configVals);
        httpPollTransport = new HttpPollTransport(config, serverEventBus, serverStatus, scheduler, httpClient, encryptedValueService);
        request = httpPollTransport.getRequestBuilder()
                .url("https://url.com")
                .build();

        assertEquals(GET, request.method());
        assertNull(request.body());

        configVals = Map.of(
                CK_HTTP_METHOD, POST,
                CK_HTTP_BODY, "body",
                CK_CONTENT_TYPE, APPLICATION_JSON);
        config = new Configuration(configVals);
        httpPollTransport = new HttpPollTransport(config, serverEventBus, serverStatus, scheduler, httpClient, encryptedValueService);
        request = httpPollTransport.getRequestBuilder()
                .url("https://url.com")
                .build();

        assertEquals(POST, request.method());
        assertEquals("body", bodyToString(request));
        assertEquals(APPLICATION_JSON, request.body().contentType().type() + "/" + request.body().contentType().subtype());

        configVals = Map.of(
                CK_HTTP_METHOD, PUT,
                CK_HTTP_BODY, "body",
                CK_CONTENT_TYPE, TEXT_PLAIN);
        config = new Configuration(configVals);
        httpPollTransport = new HttpPollTransport(config, serverEventBus, serverStatus, scheduler, httpClient, encryptedValueService);
        request = httpPollTransport.getRequestBuilder()
                .url("https://url.com")
                .build();

        assertEquals(PUT, request.method());
        assertEquals("body", bodyToString(request));
        assertEquals(TEXT_PLAIN, request.body().contentType().type() + "/" + request.body().contentType().subtype());
    }

    @SuppressWarnings("ConstantConditions")
    private static String bodyToString(final Request request) throws IOException {
        final Request copy = request.newBuilder().build();
        final Buffer buffer = new Buffer();
        copy.body().writeTo(buffer);
        return buffer.readUtf8();
    }
}
