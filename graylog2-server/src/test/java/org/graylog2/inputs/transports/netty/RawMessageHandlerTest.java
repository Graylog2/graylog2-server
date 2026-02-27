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

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.graylog2.plugin.ResolvableInetSocketAddress;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.journal.RawMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class RawMessageHandlerTest {

    @Test
    void channelAttributeNotSet() {
        // catch the raw message written out by the handler
        final MessageInput input = mock(MessageInput.class);

        // we have to override this, because EmbeddedChannel uses a different class than RawMessageHandler expects for
        // the remote address.
        final EmbeddedChannel channel = new EmbeddedChannel(new RawMessageHandler(input)) {
            @Override
            protected SocketAddress remoteAddress0() {
                return new InetSocketAddress("6.6.6.6", 0);
            }
        };

        channel.writeInbound(Unpooled.wrappedBuffer(new byte[]{1, 2, 3}));
        channel.finish();
        ArgumentCaptor<RawMessage> captor = ArgumentCaptor.forClass(RawMessage.class);
        verify(input).processRawMessage(captor.capture());
        final ResolvableInetSocketAddress actual = captor.getValue().getRemoteAddress();
        final ResolvableInetSocketAddress expected = ResolvableInetSocketAddress.wrap(new InetSocketAddress("6.6.6.6", 0));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void remoteAddressOverriddenByChannelAttribute() {
        // catch the raw message written out by the handler
        final MessageInput input = mock(MessageInput.class);

        // we have to override this, because EmbeddedChannel uses a different class than RawMessageHandler expects for
        // the remote address.
        final EmbeddedChannel channel = new EmbeddedChannel(new RawMessageHandler(input)) {
            @Override
            protected SocketAddress remoteAddress0() {
                return new InetSocketAddress("6.6.6.6", 0);
            }
        };

        channel.attr(RawMessageHandler.ORIGINAL_IP_KEY).set(new InetSocketAddress("3.3.3.3", 0));
        channel.writeInbound(Unpooled.wrappedBuffer(new byte[]{1, 2, 3}));
        channel.finish();
        ArgumentCaptor<RawMessage> captor = ArgumentCaptor.forClass(RawMessage.class);
        verify(input).processRawMessage(captor.capture());
        final ResolvableInetSocketAddress actual = captor.getValue().getRemoteAddress();
        final ResolvableInetSocketAddress expected = ResolvableInetSocketAddress.wrap(new InetSocketAddress("3.3.3.3", 0));
        assertThat(actual).isEqualTo(expected);
    }
}
