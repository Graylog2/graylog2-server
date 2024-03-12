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
package org.graylog2.shared.buffers.processors;

import de.huxhorn.sulky.ulid.ULID;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MessageULIDGeneratorTest {
    private final MessageFactory messageFactory = new TestMessageFactory();

    @Test
    public void simpleGenerate() {
        final MessageULIDGenerator generator = new MessageULIDGenerator(new ULID());

        final long ts = Tools.nowUTC().getMillis();

        ULID.Value parsedULID = ULID.parseULID(generator.createULID(ts, 123));

        assertThat(extractSequenceNr(parsedULID)).isEqualTo(123);
    }

    @Test
    public void intMaxGenerate() {
        final MessageULIDGenerator generator = new MessageULIDGenerator(new ULID());

        final long ts = Tools.nowUTC().getMillis();

        ULID.Value parsedULID = ULID.parseULID(generator.createULID(ts, Integer.MAX_VALUE));

        assertThat(extractSequenceNr(parsedULID)).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void uintMaxGenerate() {
        final MessageULIDGenerator generator = new MessageULIDGenerator(new ULID());

        final long ts = Tools.nowUTC().getMillis();

        final int uIntMaxValue = ~0;
        ULID.Value parsedULID = ULID.parseULID(generator.createULID(ts, uIntMaxValue));

        assertThat(extractSequenceNr(parsedULID)).isEqualTo(uIntMaxValue);
    }

    @Test
    public void testUlidSorting() {
        final MessageULIDGenerator generator = new MessageULIDGenerator(new ULID());
        final long ts = Tools.nowUTC().getMillis();

        final long[] wrappedLongs = new long[]{0, 1, Integer.MAX_VALUE, 0xFFFF_FFFEL, 0xFFFF_FFFFL};

        final ArrayList<String> ulids = new ArrayList<>();
        for (long seq: wrappedLongs) {
            ulids.add(generator.createULID(ts, (int) seq));
        }
        final List<String> sortedUlids = ulids.stream().sorted().collect(Collectors.toList());

        assertThat(ulids).isEqualTo(sortedUlids);
    }

    @Test
    public void keepsTimestamp() {
        final MessageULIDGenerator generator = new MessageULIDGenerator(new ULID());

        final long ts = Tools.nowUTC().getMillis();
        final String ulid = generator.createULID(ts, 123);
        ULID.Value parsedULID = ULID.parseULID(ulid);

        assertThat(parsedULID.timestamp()).isEqualTo(ts);
    }

    @Test
    public void doesNotAcceptTooLargeTimestamp() {
        final MessageULIDGenerator generator = new MessageULIDGenerator(new ULID());
        final DateTime largeDate = DateTime.parse("+10889-08-02T05:31:50.656Z");
        final Message message = messageFactory.createMessage("foo", "source", largeDate);

        assertThatThrownBy(() -> {
            generator.createULID(message);
        }).isInstanceOf(IllegalArgumentException.class);
    }

    private int extractSequenceNr(ULID.Value ulid) {
        int msb = (int) (ulid.getMostSignificantBits() & 0x000000000000FFFFL) << 16;
        return (int) (msb | ulid.getLeastSignificantBits() >>> 48);
    }
}
