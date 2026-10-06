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
package org.graylog2.cluster.nodes;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import org.bson.BsonTimestamp;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class NodeDtoTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final NodeDto.LastSeenDeserializer deserializer = new NodeDto.LastSeenDeserializer();

    @Test
    void testDeserializesEmbeddedDate() throws Exception {
        assertThat(deserialize(buf -> buf.writeEmbeddedObject(new Date(1_000_000_000L))))
                .isEqualTo(new DateTime(1_000_000_000L, DateTimeZone.UTC));
    }

    @Test
    void testDeserializesEmbeddedBsonTimestamp() throws Exception {
        // BsonTimestamp stores time as epoch seconds; the deserializer multiplies by 1000 to get millis
        assertThat(deserialize(buf -> buf.writeEmbeddedObject(new BsonTimestamp(1_000_000, 0))))
                .isEqualTo(new DateTime(1_000_000_000L, DateTimeZone.UTC));
    }

    @Test
    void testDeserializesNumericEpochSeconds() throws Exception {
        assertThat(deserialize(buf -> buf.writeNumber(1_000_000L)))
                .isEqualTo(new DateTime(1_000_000_000L, DateTimeZone.UTC));
    }

    @Test
    void testDeserializesIsoString() throws Exception {
        assertThat(deserialize(buf -> buf.writeString("2021-01-01T00:00:00.000Z")))
                .isEqualTo(new DateTime(2021, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC));
    }

    private DateTime deserialize(TokenWriter writer) throws Exception {
        final TokenBuffer buf = new TokenBuffer(mapper, false);
        writer.write(buf);
        try (JsonParser p = buf.asParser()) {
            p.nextToken();
            return deserializer.deserialize(p, null);
        }
    }

    @FunctionalInterface
    interface TokenWriter {
        void write(TokenBuffer buf) throws Exception;
    }
}
