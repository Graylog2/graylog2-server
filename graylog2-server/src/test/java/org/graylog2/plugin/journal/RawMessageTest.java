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
package org.graylog2.plugin.journal;

import jakarta.annotation.Nonnull;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.system.SimpleNodeId;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RawMessageTest {
    @Test
    void minimalEncodeDecode() {
        final RawMessage rawMessage = createRawMessage("testmessage".getBytes(StandardCharsets.UTF_8));

        final byte[] encoded = rawMessage.encode();
        final RawMessage decodedMsg = RawMessage.decode(encoded, 1);

        assertNotNull(decodedMsg);
        assertArrayEquals("testmessage".getBytes(StandardCharsets.UTF_8), decodedMsg.getPayload());
        assertEquals("raw", decodedMsg.getCodecName());
        assertEquals(1, decodedMsg.getSourceNodes().size());
        assertEquals("inputid", decodedMsg.getSourceNodes().get(0).inputId);
        assertEquals("5ca1ab1e-0000-4000-a000-000000000000", decodedMsg.getSourceNodes().get(0).nodeId);
    }

    @Test
    void inputMessageSizeSurvivesJournalRoundTrip() {
        final RawMessage rawMessage = createRawMessage("testmessage".getBytes(StandardCharsets.UTF_8));
        rawMessage.setInputMessageSize(12345);

        final byte[] encoded = rawMessage.encode();
        final RawMessage decodedMsg = RawMessage.decode(encoded, 1);

        assertNotNull(decodedMsg);
        assertEquals(12345, decodedMsg.getInputMessageSize());
    }

    @Test
    void getInputMessageSizeFallsBackToPayloadSize() {
        final byte[] payload = "testmessage".getBytes(StandardCharsets.UTF_8);
        final RawMessage rawMessage = createRawMessage(payload);

        final byte[] encoded = rawMessage.encode();
        final RawMessage decodedMsg = RawMessage.decode(encoded, 1);

        assertNotNull(decodedMsg);
        assertEquals(payload.length, decodedMsg.getInputMessageSize());
        assertEquals(payload.length, decodedMsg.getPayloadSize());
    }

    @Nonnull
    private RawMessage createRawMessage(byte[] payload) {
        final RawMessage rawMessage = new RawMessage(payload);
        rawMessage.addSourceNode("inputid", new SimpleNodeId("5ca1ab1e-0000-4000-a000-000000000000"));
        rawMessage.setCodecName("raw");
        rawMessage.setCodecConfig(Configuration.EMPTY_CONFIGURATION);
        return rawMessage;
    }
}
