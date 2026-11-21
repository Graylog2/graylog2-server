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
package org.graylog.plugins.beats;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Events larger than Jackson's default string length limit (20 million characters) must make it through the frame
 * decoder and the codecs when the max. event size allows them. The frame decoder acknowledges frames before the codec
 * parses them, so a codec failure would drop events the sender considers delivered.
 */
class BeatsLargeEventTest {
    private static final int MESSAGE_LENGTH = 21_000_000;
    private static final int MAX_EVENT_SIZE = 32 * 1024 * 1024;

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private final MessageFactory messageFactory = new TestMessageFactory();

    @Test
    void beats2CodecDecodesEventWithStringLongerThanJacksonDefaultLimit() {
        final Beats2Codec codec = new Beats2Codec(Configuration.EMPTY_CONFIGURATION, objectMapper, messageFactory);

        final Message message = codec.decodeSafe(new RawMessage(decodeFrame(largeEvent()))).orElseThrow();

        assertThat(message.getMessage()).hasSize(MESSAGE_LENGTH);
    }

    @Test
    void beatsCodecDecodesEventWithStringLongerThanJacksonDefaultLimit() {
        final BeatsCodec codec = new BeatsCodec(Configuration.EMPTY_CONFIGURATION, objectMapper, messageFactory);

        final Message message = codec.decodeSafe(new RawMessage(decodeFrame(largeEvent()))).orElseThrow();

        assertThat(message.getMessage()).hasSize(MESSAGE_LENGTH);
    }

    private static byte[] largeEvent() {
        return ("{\"@timestamp\":\"2016-04-01T00:00:00.000Z\",\"message\":\"" + "X".repeat(MESSAGE_LENGTH) + "\"}")
                .getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] decodeFrame(byte[] json) {
        final ByteBuf frame = Unpooled.buffer(10 + json.length);
        frame.writeByte('2');
        frame.writeByte('J');
        frame.writeInt(0);
        frame.writeInt(json.length);
        frame.writeBytes(json);

        final EmbeddedChannel channel = new EmbeddedChannel(new BeatsFrameDecoder(MAX_EVENT_SIZE));
        try {
            channel.writeInbound(frame);

            final ByteBuf decoded = channel.readInbound();
            try {
                return ByteBufUtil.getBytes(decoded);
            } finally {
                decoded.release();
            }
        } finally {
            // Also releases the ACK the decoder wrote
            channel.finishAndReleaseAll();
        }
    }
}
