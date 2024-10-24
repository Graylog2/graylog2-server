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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.AddressedEnvelope;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.journal.RawMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnvelopeMessageHandlerTest {
    private static final String MESSAGE_UNTRIMMED = "message \r\n";
    private static final String MESSAGE_TRIMMED = "message";
    @Mock
    private MessageInput input;
    @Mock
    private AddressedEnvelope<ByteBuf, InetSocketAddress> envelope;

    @Test
    public void testHandleMessage() throws Exception {
        final EnvelopeMessageHandler handler = new EnvelopeMessageHandler(input, false);
        when(envelope.content()).thenReturn(Unpooled.wrappedBuffer(MESSAGE_TRIMMED.getBytes(Charset.defaultCharset())));
        final ArgumentCaptor<RawMessage> captor = ArgumentCaptor.forClass(RawMessage.class);
        doNothing().when(input).processRawMessage(captor.capture());
        handler.channelRead0(null, envelope);
        Assertions.assertEquals(MESSAGE_TRIMMED, new String(captor.getValue().getPayload(), Charset.defaultCharset()));
    }

    @Test
    public void testHandleTrimMessage() throws Exception {
        final EnvelopeMessageHandler handler = new EnvelopeMessageHandler(input, true);
        when(envelope.content()).thenReturn(Unpooled.wrappedBuffer(MESSAGE_UNTRIMMED.getBytes(Charset.defaultCharset())));
        final ArgumentCaptor<RawMessage> captor = ArgumentCaptor.forClass(RawMessage.class);
        doNothing().when(input).processRawMessage(captor.capture());
        handler.channelRead0(null, envelope);
        // Verify that trimmed message is emitted.
        Assertions.assertEquals(MESSAGE_TRIMMED, new String(captor.getValue().getPayload(), Charset.defaultCharset()));
    }
}
