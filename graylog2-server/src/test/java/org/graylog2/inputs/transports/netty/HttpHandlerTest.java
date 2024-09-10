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
package org.graylog2.inputs.transports.netty;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AsciiString;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static io.netty.handler.codec.http.HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS;
import static io.netty.handler.codec.http.HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS;
import static io.netty.handler.codec.http.HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN;
import static io.netty.handler.codec.http.HttpHeaderNames.AUTHORIZATION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.HOST;
import static io.netty.handler.codec.http.HttpHeaderNames.ORIGIN;
import static io.netty.handler.codec.http.HttpResponseStatus.ACCEPTED;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;
import static org.assertj.core.api.Assertions.assertThat;

public class HttpHandlerTest {
    private static final byte[] GELF_MESSAGE = "{\"version\":\"1.1\",\"short_message\":\"Foo\",\"host\":\"localhost\"}".getBytes(StandardCharsets.UTF_8);
    private static final String BEARER_EXPECTED_TOKEN = "Bearer: expected-token";
    private EmbeddedChannel channel;

    @Before
    public void setUp() {
        channel = new EmbeddedChannel(new HttpHandler(true, null, null, "/gelf"));
    }

    @Test
    public void messageReceivedSuccessfullyProcessesPOSTRequest() {
        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/gelf");
        httpRequest.headers().add(HOST, "localhost");
        httpRequest.headers().add(ORIGIN, "http://example.com");
        httpRequest.headers().add(CONNECTION, HttpHeaderValues.CLOSE);

        httpRequest.content().writeBytes(GELF_MESSAGE);

        channel.writeInbound(httpRequest);
        channel.finish();

        final HttpResponse httpResponse = channel.readOutbound();
        assertThat(httpResponse.status()).isEqualTo(ACCEPTED);
        final HttpHeaders headers = httpResponse.headers();
        assertThat(headers.get(CONTENT_LENGTH)).isEqualTo("0");
        assertThat(headers.get(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("http://example.com");
        assertThat(headers.get(ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
        assertThat(headers.get(ACCESS_CONTROL_ALLOW_HEADERS)).isEqualTo("Authorization, Content-Type");
        assertThat(headers.get(CONNECTION)).isEqualTo(HttpHeaderValues.CLOSE.toString());
    }

    @Test
    public void withKeepalive() {
        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/gelf");
        httpRequest.headers().add(HOST, "localhost");
        httpRequest.headers().add(CONNECTION, HttpHeaderValues.KEEP_ALIVE);

        httpRequest.content().writeBytes(GELF_MESSAGE);

        channel.writeInbound(httpRequest);
        channel.finish();

        final HttpResponse httpResponse = channel.readOutbound();
        assertThat(httpResponse.status()).isEqualTo(ACCEPTED);
        final HttpHeaders headers = httpResponse.headers();
        assertThat(headers.get(CONTENT_LENGTH)).isEqualTo("0");
        assertThat(headers.get(CONNECTION)).isEqualTo(HttpHeaderValues.KEEP_ALIVE.toString());
    }

    @Test
    public void withJSONContentType() {
        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/gelf");
        httpRequest.headers().add(HOST, "localhost");
        httpRequest.headers().add(CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
        httpRequest.headers().add(CONNECTION, HttpHeaderValues.CLOSE);

        httpRequest.content().writeBytes(GELF_MESSAGE);

        channel.writeInbound(httpRequest);
        channel.finish();

        final HttpResponse httpResponse = channel.readOutbound();
        assertThat(httpResponse.status()).isEqualTo(ACCEPTED);
        final HttpHeaders headers = httpResponse.headers();
        assertThat(headers.get(CONTENT_LENGTH)).isEqualTo("0");
        assertThat(headers.get(CONNECTION)).isEqualTo(HttpHeaderValues.CLOSE.toString());
    }

    @Test
    public void withCustomContentType() {
        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/gelf");
        httpRequest.headers().add(HOST, "localhost");
        httpRequest.headers().add(CONTENT_TYPE, "foo/bar");
        httpRequest.headers().add(CONNECTION, HttpHeaderValues.CLOSE);

        httpRequest.content().writeBytes(GELF_MESSAGE);

        channel.writeInbound(httpRequest);
        channel.finish();

        final HttpResponse httpResponse = channel.readOutbound();
        assertThat(httpResponse.status()).isEqualTo(ACCEPTED);
        final HttpHeaders headers = httpResponse.headers();
        assertThat(headers.get(CONTENT_LENGTH)).isEqualTo("0");
        assertThat(headers.get(CONNECTION)).isEqualTo(HttpHeaderValues.CLOSE.toString());
    }

    @Test
    public void successfullyProcessOPTIONSRequest() {
        final HttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.OPTIONS, "/gelf");
        httpRequest.headers().add(HOST, "localhost");
        httpRequest.headers().add(ORIGIN, "http://example.com");
        httpRequest.headers().add(CONNECTION, HttpHeaderValues.CLOSE);

        channel.writeInbound(httpRequest);
        channel.finish();

        final HttpResponse httpResponse = channel.readOutbound();
        assertThat(httpResponse.status()).isEqualTo(HttpResponseStatus.OK);

        final HttpHeaders headers = httpResponse.headers();
        assertThat(headers.get(CONTENT_LENGTH)).isEqualTo("0");
        assertThat(headers.get(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("http://example.com");
        assertThat(headers.get(ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
        assertThat(headers.get(ACCESS_CONTROL_ALLOW_HEADERS)).isEqualTo("Authorization, Content-Type");
    }

    @Test
    public void successfullyProcessOPTIONSRequestWithoutOrigin() {
        final HttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.OPTIONS, "/gelf");
        httpRequest.headers().add(HOST, "localhost");
        httpRequest.headers().add(CONNECTION, HttpHeaderValues.CLOSE);

        channel.writeInbound(httpRequest);
        channel.finish();

        final HttpResponse httpResponse = channel.readOutbound();
        assertThat(httpResponse.status()).isEqualTo(HttpResponseStatus.OK);

        final HttpHeaders headers = httpResponse.headers();
        assertThat(headers.get(CONTENT_LENGTH)).isEqualTo("0");
        assertThat(headers.contains(ACCESS_CONTROL_ALLOW_ORIGIN)).isFalse();
        assertThat(headers.contains(ACCESS_CONTROL_ALLOW_CREDENTIALS)).isFalse();
        assertThat(headers.contains(ACCESS_CONTROL_ALLOW_HEADERS)).isFalse();
    }

    @Test
    public void return404ForWrongPath() {
        final HttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/");
        httpRequest.headers().add(HOST, "localhost");
        httpRequest.headers().add(ORIGIN, "http://example.com");
        httpRequest.headers().add(CONNECTION, HttpHeaderValues.CLOSE);

        channel.writeInbound(httpRequest);
        channel.finish();

        final HttpResponse httpResponse = channel.readOutbound();
        assertThat(httpResponse.status()).isEqualTo(HttpResponseStatus.NOT_FOUND);
        final HttpHeaders headers = httpResponse.headers();
        assertThat(headers.get(CONTENT_LENGTH)).isEqualTo("0");
        assertThat(headers.get(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("http://example.com");
        assertThat(headers.get(ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
        assertThat(headers.get(ACCESS_CONTROL_ALLOW_HEADERS)).isEqualTo("Authorization, Content-Type");
    }

    @Test
    public void messageReceivedReturns405ForInvalidMethod() {
        final HttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
        httpRequest.headers().add(HOST, "localhost");
        httpRequest.headers().add(ORIGIN, "http://example.com");
        httpRequest.headers().add(CONNECTION, HttpHeaderValues.CLOSE);

        channel.writeInbound(httpRequest);
        channel.finish();

        final HttpResponse httpResponse = channel.readOutbound();
        assertThat(httpResponse.status()).isEqualTo(HttpResponseStatus.METHOD_NOT_ALLOWED);
        final HttpHeaders headers = httpResponse.headers();
        assertThat(headers.get(CONTENT_LENGTH)).isEqualTo("0");
        assertThat(headers.get(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("http://example.com");
        assertThat(headers.get(ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
        assertThat(headers.get(ACCESS_CONTROL_ALLOW_HEADERS)).isEqualTo("Authorization, Content-Type");
    }

    @Test
    public void testAuthentication() {
        // No auth required - success.
        testAuthentication(null, null, null, null, ACCEPTED);
        // Auth required - success.
        testAuthentication(AUTHORIZATION.toString(), BEARER_EXPECTED_TOKEN, AUTHORIZATION, BEARER_EXPECTED_TOKEN, ACCEPTED);
        // Auth required - failures.
        testAuthentication(AUTHORIZATION.toString(), BEARER_EXPECTED_TOKEN, AUTHORIZATION, "bad-token", UNAUTHORIZED);
        testAuthentication(AUTHORIZATION.toString(), BEARER_EXPECTED_TOKEN, AUTHORIZATION, "", UNAUTHORIZED);
        testAuthentication(AUTHORIZATION.toString(), BEARER_EXPECTED_TOKEN, null, "", UNAUTHORIZED);
    }

    private void testAuthentication(String expectedAuthHeader, String expectedAuthHeaderValue, AsciiString suppliedAuthHeader, String suppliedAuthHeaderValue,
                                    HttpResponseStatus expectedStatus) {
        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/gelf");
        httpRequest.headers().add(HOST, "localhost");
        httpRequest.headers().add(ORIGIN, "http://example.com");
        httpRequest.headers().add(CONNECTION, HttpHeaderValues.CLOSE);
        if (suppliedAuthHeader != null) {
            httpRequest.headers().add(suppliedAuthHeader, suppliedAuthHeaderValue);
        }

        httpRequest.content().writeBytes(GELF_MESSAGE);

        channel = new EmbeddedChannel(new HttpHandler(true, expectedAuthHeader, expectedAuthHeaderValue, "/gelf"));
        channel.writeInbound(httpRequest);
        channel.finish();

        final HttpResponse httpResponse = channel.readOutbound();
        // Request should be successful.
        assertThat(httpResponse.status()).isEqualTo(expectedStatus);
        final HttpHeaders headers = httpResponse.headers();
        assertThat(headers.get(CONTENT_LENGTH)).isEqualTo("0");
        assertThat(headers.get(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("http://example.com");
        assertThat(headers.get(ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
        assertThat(headers.get(ACCESS_CONTROL_ALLOW_HEADERS)).isEqualTo("Authorization, Content-Type");
        assertThat(headers.get(CONNECTION)).isEqualTo(HttpHeaderValues.CLOSE.toString());
    }
}
