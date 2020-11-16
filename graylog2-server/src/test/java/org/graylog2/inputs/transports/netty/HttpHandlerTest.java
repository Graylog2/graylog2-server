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
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpHandlerTest {
    private static final byte[] GELF_MESSAGE = "{\"version\":\"1.1\",\"short_message\":\"Foo\",\"host\":\"localhost\"}".getBytes(StandardCharsets.UTF_8);
    private EmbeddedChannel channel;

    @Before
    public void setUp() {
        channel = new EmbeddedChannel(new HttpHandler(true));
    }

    @Test
    public void messageReceivedSuccessfullyProcessesPOSTRequest() {
        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/gelf");
        httpRequest.headers().add(HttpHeaderNames.HOST, "localhost");
        httpRequest.headers().add(HttpHeaderNames.ORIGIN, "http://example.com");
        httpRequest.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

        httpRequest.content().writeBytes(GELF_MESSAGE);

        channel.writeInbound(httpRequest);
        channel.finish();

        final HttpResponse httpResponse = channel.readOutbound();
        assertThat(httpResponse.status()).isEqualTo(HttpResponseStatus.ACCEPTED);
        final HttpHeaders headers = httpResponse.headers();
        assertThat(headers.get(HttpHeaderNames.CONTENT_LENGTH)).isEqualTo("0");
        assertThat(headers.get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("http://example.com");
        assertThat(headers.get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
        assertThat(headers.get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS)).isEqualTo("Authorization, Content-Type");
        assertThat(headers.get(HttpHeaderNames.CONNECTION)).isEqualTo(HttpHeaderValues.CLOSE.toString());
    }


    @Test
    public void withKeepalive() {
        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/gelf");
        httpRequest.headers().add(HttpHeaderNames.HOST, "localhost");
        httpRequest.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

        httpRequest.content().writeBytes(GELF_MESSAGE);

        channel.writeInbound(httpRequest);
        channel.finish();

        final HttpResponse httpResponse = channel.readOutbound();
        assertThat(httpResponse.status()).isEqualTo(HttpResponseStatus.ACCEPTED);
        final HttpHeaders headers = httpResponse.headers();
        assertThat(headers.get(HttpHeaderNames.CONTENT_LENGTH)).isEqualTo("0");
        assertThat(headers.get(HttpHeaderNames.CONNECTION)).isEqualTo(HttpHeaderValues.KEEP_ALIVE.toString());
    }

    @Test
    public void withJSONContentType() {
        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/gelf");
        httpRequest.headers().add(HttpHeaderNames.HOST, "localhost");
        httpRequest.headers().add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
        httpRequest.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

        httpRequest.content().writeBytes(GELF_MESSAGE);

        channel.writeInbound(httpRequest);
        channel.finish();

        final HttpResponse httpResponse = channel.readOutbound();
        assertThat(httpResponse.status()).isEqualTo(HttpResponseStatus.ACCEPTED);
        final HttpHeaders headers = httpResponse.headers();
        assertThat(headers.get(HttpHeaderNames.CONTENT_LENGTH)).isEqualTo("0");
        assertThat(headers.get(HttpHeaderNames.CONNECTION)).isEqualTo(HttpHeaderValues.CLOSE.toString());
    }

    @Test
    public void withCustomContentType() {
        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/gelf");
        httpRequest.headers().add(HttpHeaderNames.HOST, "localhost");
        httpRequest.headers().add(HttpHeaderNames.CONTENT_TYPE, "foo/bar");
        httpRequest.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

        httpRequest.content().writeBytes(GELF_MESSAGE);

        channel.writeInbound(httpRequest);
        channel.finish();

        final HttpResponse httpResponse = channel.readOutbound();
        assertThat(httpResponse.status()).isEqualTo(HttpResponseStatus.ACCEPTED);
        final HttpHeaders headers = httpResponse.headers();
        assertThat(headers.get(HttpHeaderNames.CONTENT_LENGTH)).isEqualTo("0");
        assertThat(headers.get(HttpHeaderNames.CONNECTION)).isEqualTo(HttpHeaderValues.CLOSE.toString());
    }

    @Test
    public void successfullyProcessOPTIONSRequest() {
        final HttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.OPTIONS, "/gelf");
        httpRequest.headers().add(HttpHeaderNames.HOST, "localhost");
        httpRequest.headers().add(HttpHeaderNames.ORIGIN, "http://example.com");
        httpRequest.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

        channel.writeInbound(httpRequest);
        channel.finish();

        final HttpResponse httpResponse = channel.readOutbound();
        assertThat(httpResponse.status()).isEqualTo(HttpResponseStatus.OK);

        final HttpHeaders headers = httpResponse.headers();
        assertThat(headers.get(HttpHeaderNames.CONTENT_LENGTH)).isEqualTo("0");
        assertThat(headers.get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("http://example.com");
        assertThat(headers.get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
        assertThat(headers.get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS)).isEqualTo("Authorization, Content-Type");
    }

    @Test
    public void successfullyProcessOPTIONSRequestWithoutOrigin() {
        final HttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.OPTIONS, "/gelf");
        httpRequest.headers().add(HttpHeaderNames.HOST, "localhost");
        httpRequest.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

        channel.writeInbound(httpRequest);
        channel.finish();

        final HttpResponse httpResponse = channel.readOutbound();
        assertThat(httpResponse.status()).isEqualTo(HttpResponseStatus.OK);

        final HttpHeaders headers = httpResponse.headers();
        assertThat(headers.get(HttpHeaderNames.CONTENT_LENGTH)).isEqualTo("0");
        assertThat(headers.contains(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN)).isFalse();
        assertThat(headers.contains(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isFalse();
        assertThat(headers.contains(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS)).isFalse();
    }

    @Test
    public void return404ForWrongPath() {
        final HttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/");
        httpRequest.headers().add(HttpHeaderNames.HOST, "localhost");
        httpRequest.headers().add(HttpHeaderNames.ORIGIN, "http://example.com");
        httpRequest.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

        channel.writeInbound(httpRequest);
        channel.finish();

        final HttpResponse httpResponse = channel.readOutbound();
        assertThat(httpResponse.status()).isEqualTo(HttpResponseStatus.NOT_FOUND);
        final HttpHeaders headers = httpResponse.headers();
        assertThat(headers.get(HttpHeaderNames.CONTENT_LENGTH)).isEqualTo("0");
        assertThat(headers.get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("http://example.com");
        assertThat(headers.get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
        assertThat(headers.get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS)).isEqualTo("Authorization, Content-Type");
    }

    @Test
    public void messageReceivedReturns405ForInvalidMethod() {
        final HttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
        httpRequest.headers().add(HttpHeaderNames.HOST, "localhost");
        httpRequest.headers().add(HttpHeaderNames.ORIGIN, "http://example.com");
        httpRequest.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

        channel.writeInbound(httpRequest);
        channel.finish();

        final HttpResponse httpResponse = channel.readOutbound();
        assertThat(httpResponse.status()).isEqualTo(HttpResponseStatus.METHOD_NOT_ALLOWED);
        final HttpHeaders headers = httpResponse.headers();
        assertThat(headers.get(HttpHeaderNames.CONTENT_LENGTH)).isEqualTo("0");
        assertThat(headers.get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("http://example.com");
        assertThat(headers.get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
        assertThat(headers.get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS)).isEqualTo("Authorization, Content-Type");
    }

}