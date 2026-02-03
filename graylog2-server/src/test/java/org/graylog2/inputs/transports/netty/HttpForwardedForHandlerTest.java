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
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import org.graylog2.utilities.IpSubnet;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class HttpForwardedForHandlerTest {

    public static final byte[] EMPTY_PAYLOAD = "".getBytes(StandardCharsets.UTF_8);

    @Test
    void disabledHandlerHasNoSideEffects() {
        final EmbeddedChannel channel = new EmbeddedChannel(
                new HttpForwardedForHandler(false, false, "", false, Set.of())
        );

        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/raw");
        // payload is irrelevant for this test
        httpRequest.content().writeBytes(EMPTY_PAYLOAD);

        final FullHttpRequest originalRequest = httpRequest.copy();

        channel.writeInbound(httpRequest);
        channel.finish();

        // the handler shouldn't have any side effects
        final HttpRequest o = channel.readInbound();
        assertThat(o).isEqualTo(originalRequest);
        assertThat(channel.hasAttr(RawMessageHandler.ORIGINAL_IP_KEY)).isFalse();
    }

    @Test
    void xForwardedForDecoded() {
        final EmbeddedChannel channel = new EmbeddedChannel(
                new HttpForwardedForHandler(true, false, "", false, Set.of())
        );
        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/raw");
        httpRequest.headers().add("X-Forwarded-For", "6.6.6.6, 192.168.1.1");
        // payload is irrelevant for this test
        httpRequest.content().writeBytes(EMPTY_PAYLOAD);

        channel.writeInbound(httpRequest);
        channel.finish();
        assertThat(channel.hasAttr(RawMessageHandler.ORIGINAL_IP_KEY)).isTrue();
        assertThat(channel.attr(RawMessageHandler.ORIGINAL_IP_KEY).get()).isEqualTo(new InetSocketAddress("6.6.6.6", 0));
    }

    @Test
    void forwardedDecoded() {
        final EmbeddedChannel channel = new EmbeddedChannel(
                new HttpForwardedForHandler(true, false, "", false, Set.of())
        );
        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/raw");
        httpRequest.headers().add("Forwarded", "for=6.6.6.6, by=192.168.1.1");
        httpRequest.headers().add("Forwarded", "by=5.4.2.2");
        httpRequest.headers().add("Forwarded", "for=192.168.1.1, by=10.0.1.20");
        // payload is irrelevant for this test
        httpRequest.content().writeBytes(EMPTY_PAYLOAD);

        channel.writeInbound(httpRequest);
        channel.finish();
        assertThat(channel.hasAttr(RawMessageHandler.ORIGINAL_IP_KEY)).isTrue();
        assertThat(channel.attr(RawMessageHandler.ORIGINAL_IP_KEY).get()).isEqualTo(new InetSocketAddress("6.6.6.6", 0));
    }

    @Test
    void realIp() {
        final EmbeddedChannel channel = new EmbeddedChannel(
                new HttpForwardedForHandler(true,
                        true,
                        "X-Real-Ip, X-Client-IP, CF-Connecting-IP, True-Client-IP, Fastly-Client-IP",
                        false, Set.of())
        );
        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/raw");
        httpRequest.headers().add("X-Real-Ip", "6.6.6.6");
        // payload is irrelevant for this test
        httpRequest.content().writeBytes(EMPTY_PAYLOAD);

        channel.writeInbound(httpRequest);
        channel.finish();
        assertThat(channel.hasAttr(RawMessageHandler.ORIGINAL_IP_KEY)).isTrue();
        assertThat(channel.attr(RawMessageHandler.ORIGINAL_IP_KEY).get()).isEqualTo(new InetSocketAddress("6.6.6.6", 0));
    }

    @Test
    void oneOfRealIpHeaders() {
        final EmbeddedChannel channel = new EmbeddedChannel(
                new HttpForwardedForHandler(true,
                        true,
                        "X-Real-Ip, X-Client-IP, CF-Connecting-IP, True-Client-IP, Fastly-Client-IP",
                        false, Set.of())
        );
        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/raw");
        httpRequest.headers().add("true-client-ip", "6.6.6.6");
        // payload is irrelevant for this test
        httpRequest.content().writeBytes(EMPTY_PAYLOAD);

        channel.writeInbound(httpRequest);
        channel.finish();
        assertThat(channel.hasAttr(RawMessageHandler.ORIGINAL_IP_KEY)).isTrue();
        assertThat(channel.attr(RawMessageHandler.ORIGINAL_IP_KEY).get()).isEqualTo(new InetSocketAddress("6.6.6.6", 0));
    }

    @Test
    void xForwardedForHonorsTrustedProxies() throws UnknownHostException {
        final IpSubnet host192 = new IpSubnet("192.168.1.1/32");
        final IpSubnet net10_0_0 = new IpSubnet("10.0.0.1/24");
        final EmbeddedChannel channel = new EmbeddedChannel(
                new HttpForwardedForHandler(true,
                        false,
                        "",
                        true, Set.of(host192, net10_0_0))
        );
        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/raw");
        httpRequest.headers().add("X-Forwarded-For", "6.6.6.6, 10.0.0.24, 192.168.1.1");
        // payload is irrelevant for this test
        httpRequest.content().writeBytes(EMPTY_PAYLOAD);

        channel.writeInbound(httpRequest);
        channel.finish();
        assertThat(channel.hasAttr(RawMessageHandler.ORIGINAL_IP_KEY)).isTrue();
        assertThat(channel.attr(RawMessageHandler.ORIGINAL_IP_KEY).get()).isEqualTo(new InetSocketAddress("6.6.6.6", 0));
    }

    @Test
    void xForwardedForHonorsTrustedProxiesOneUntrusted() throws UnknownHostException {
        final IpSubnet host192 = new IpSubnet("192.168.1.1/32");
        final IpSubnet net10_0_0 = new IpSubnet("10.0.0.1/24");
        final EmbeddedChannel channel = new EmbeddedChannel(
                new HttpForwardedForHandler(true,
                        false,
                        "",
                        true, Set.of(host192, net10_0_0))
        );
        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/raw");
        // there's a proxy that we don't trust
        httpRequest.headers().add("X-Forwarded-For", "6.6.6.6, 10.1.0.24, 192.168.1.1");
        // payload is irrelevant for this test
        httpRequest.content().writeBytes(EMPTY_PAYLOAD);

        channel.writeInbound(httpRequest);
        channel.finish();
        assertThat(channel.hasAttr(RawMessageHandler.ORIGINAL_IP_KEY)).isFalse();
    }


    @Test
    void xForwardedForHonorsTrustedProxiesNoneTrusted() throws UnknownHostException {
        final IpSubnet host192 = new IpSubnet("192.168.1.1/32");
        final IpSubnet net10_0_0 = new IpSubnet("10.0.0.1/24");
        final EmbeddedChannel channel = new EmbeddedChannel(
                new HttpForwardedForHandler(true,
                        false,
                        "",
                        true, Set.of(host192, net10_0_0))
        );
        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/raw");
        // there's a proxy that we don't trust
        httpRequest.headers().add("X-Forwarded-For", "6.6.6.6, 10.1.0.24, 192.168.2.1");
        // payload is irrelevant for this test
        httpRequest.content().writeBytes(EMPTY_PAYLOAD);

        channel.writeInbound(httpRequest);
        channel.finish();
        assertThat(channel.hasAttr(RawMessageHandler.ORIGINAL_IP_KEY)).isFalse();
    }

    @Test
    void forwardedHonorsTrustedProxies() throws UnknownHostException {
        final IpSubnet host192 = new IpSubnet("192.168.1.1/32");
        final IpSubnet net10_0_0 = new IpSubnet("10.0.0.1/24");
        final EmbeddedChannel channel = new EmbeddedChannel(
                new HttpForwardedForHandler(true,
                        false,
                        "",
                        true, Set.of(host192, net10_0_0))
        );
        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/raw");
        httpRequest.headers().add("Forwarded", "for=6.6.6.6, for=192.168.1.1");
        httpRequest.headers().add("Forwarded", "for=10.0.0.24");
        // payload is irrelevant for this test
        httpRequest.content().writeBytes(EMPTY_PAYLOAD);

        channel.writeInbound(httpRequest);
        channel.finish();
        assertThat(channel.hasAttr(RawMessageHandler.ORIGINAL_IP_KEY)).isTrue();
        assertThat(channel.attr(RawMessageHandler.ORIGINAL_IP_KEY).get()).isEqualTo(new InetSocketAddress("6.6.6.6", 0));
    }


    @Test
    void forwardedHonorsTrustedProxiesOneUntrusted() throws UnknownHostException {
        final IpSubnet host192 = new IpSubnet("192.168.1.1/32");
        final IpSubnet net10_0_0 = new IpSubnet("10.0.0.1/24");
        final EmbeddedChannel channel = new EmbeddedChannel(
                new HttpForwardedForHandler(true,
                        false,
                        "",
                        true, Set.of(host192, net10_0_0))
        );
        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/raw");
        // there's a proxy that we don't trust
        httpRequest.headers().add("Forwarded", "for=6.6.6.6, for=192.168.2.1");
        httpRequest.headers().add("Forwarded", "for=10.0.0.24");
        // payload is irrelevant for this test
        httpRequest.content().writeBytes(EMPTY_PAYLOAD);

        channel.writeInbound(httpRequest);
        channel.finish();
        assertThat(channel.hasAttr(RawMessageHandler.ORIGINAL_IP_KEY)).isFalse();
    }


    @Test
    void forwardedHonorsTrustedProxiesNoneTrusted() throws UnknownHostException {
        final IpSubnet host192 = new IpSubnet("192.168.1.1/32");
        final IpSubnet net10_0_0 = new IpSubnet("10.0.0.1/24");
        final EmbeddedChannel channel = new EmbeddedChannel(
                new HttpForwardedForHandler(true,
                        false,
                        "",
                        true, Set.of(host192, net10_0_0))
        );
        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/raw");
        // there's a proxy that we don't trust
        httpRequest.headers().add("Forwarded", "for=6.6.6.6, for=192.168.2.1");
        httpRequest.headers().add("Forwarded", "for=10.1.0.24");
        // payload is irrelevant for this test
        httpRequest.content().writeBytes(EMPTY_PAYLOAD);

        channel.writeInbound(httpRequest);
        channel.finish();
        assertThat(channel.hasAttr(RawMessageHandler.ORIGINAL_IP_KEY)).isFalse();
    }

}
