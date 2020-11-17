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
package org.graylog2.plugin.inputs.util;

import com.codahale.metrics.Gauge;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ThroughputCounterTest {
    private EventLoopGroup eventLoopGroup;
    private EmbeddedChannel channel;
    private ThroughputCounter throughputCounter;

    @Before
    public void setUp() {
        eventLoopGroup = new NioEventLoopGroup(4);
        throughputCounter = new ThroughputCounter(eventLoopGroup);
        channel = new EmbeddedChannel(throughputCounter);
    }

    @After
    public void tearDown() {
        eventLoopGroup.shutdownGracefully();
        channel.close().syncUninterruptibly();
    }

    @Test
    public void counterReturnsZeroIfNoInteraction() {
        channel.finish();

        final Map<String, Gauge<Long>> gauges = throughputCounter.gauges();
        assertThat(gauges.get(ThroughputCounter.READ_BYTES_1_SEC).getValue()).isEqualTo(0L);
        assertThat(gauges.get(ThroughputCounter.WRITTEN_BYTES_1_SEC).getValue()).isEqualTo(0L);
        assertThat(gauges.get(ThroughputCounter.READ_BYTES_TOTAL).getValue()).isEqualTo(0L);
        assertThat(gauges.get(ThroughputCounter.WRITTEN_BYTES_TOTAL).getValue()).isEqualTo(0L);
    }

    @Test
    @Ignore("Flaky test")
    public void counterReturnsReadBytes() throws InterruptedException {
        final ByteBuf byteBuf = Unpooled.copiedBuffer("Test", StandardCharsets.US_ASCII);
        channel.writeInbound(byteBuf);
        Thread.sleep(1000L);
        channel.writeInbound(byteBuf);
        channel.finish();


        final Map<String, Gauge<Long>> gauges = throughputCounter.gauges();
        assertThat(gauges.get(ThroughputCounter.READ_BYTES_1_SEC).getValue()).isEqualTo(4L);
        assertThat(gauges.get(ThroughputCounter.WRITTEN_BYTES_1_SEC).getValue()).isEqualTo(0L);
        assertThat(gauges.get(ThroughputCounter.READ_BYTES_TOTAL).getValue()).isEqualTo(8L);
        assertThat(gauges.get(ThroughputCounter.WRITTEN_BYTES_TOTAL).getValue()).isEqualTo(0L);
    }

    @Test
    @Ignore("Flaky test")
    public void counterReturnsWrittenBytes() throws InterruptedException {
        final ByteBuf byteBuf = Unpooled.copiedBuffer("Test", StandardCharsets.US_ASCII);
        channel.writeOutbound(byteBuf);
        Thread.sleep(1000L);
        channel.writeOutbound(byteBuf);
        channel.finish();


        final Map<String, Gauge<Long>> gauges = throughputCounter.gauges();
        assertThat(gauges.get(ThroughputCounter.READ_BYTES_1_SEC).getValue()).isEqualTo(0L);
        assertThat(gauges.get(ThroughputCounter.WRITTEN_BYTES_1_SEC).getValue()).isEqualTo(4L);
        assertThat(gauges.get(ThroughputCounter.READ_BYTES_TOTAL).getValue()).isEqualTo(0L);
        assertThat(gauges.get(ThroughputCounter.WRITTEN_BYTES_TOTAL).getValue()).isEqualTo(8L);
    }

    @Test
    public void counterReturns4Gauges() {
        assertThat(throughputCounter.gauges()).hasSize(4);
    }
}