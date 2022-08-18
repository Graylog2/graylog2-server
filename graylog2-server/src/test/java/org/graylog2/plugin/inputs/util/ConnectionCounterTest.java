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

import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

public class ConnectionCounterTest {

    @Test
    public void testConnectAndDisconnect() throws Exception {
        final ConnectionCounter connectionCounter = new ConnectionCounter(new AtomicInteger(), new AtomicLong());

        // Fresh channel, no connections so far
        assertThat(connectionCounter.getTotalConnections()).isEqualTo(0L);
        assertThat(connectionCounter.getConnectionCount()).isEqualTo(0);

        final EmbeddedChannel embeddedChannel = new EmbeddedChannel(connectionCounter);
        assertThat(embeddedChannel.isActive()).isTrue();

        // One client active
        assertThat(connectionCounter.getTotalConnections()).isEqualTo(1L);
        assertThat(connectionCounter.getConnectionCount()).isEqualTo(1);

        embeddedChannel.disconnect().get();
        assertThat(embeddedChannel.isActive()).isFalse();

        // No client, but 1 connection so far
        assertThat(connectionCounter.getTotalConnections()).isEqualTo(1L);
        assertThat(connectionCounter.getConnectionCount()).isEqualTo(0);
    }
}
