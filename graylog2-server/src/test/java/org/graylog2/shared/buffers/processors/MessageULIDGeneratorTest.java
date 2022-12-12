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
import org.graylog2.plugin.Tools;
import org.junit.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.shared.buffers.processors.MessageULIDGenerator.REORDERING_GAP;
import static org.graylog2.shared.buffers.processors.MessageULIDGenerator.ULID_RANDOM_MSB_MASK;

public class MessageULIDGeneratorTest {

    @Test
    public void simpleGenerate() {
        final MessageULIDGenerator generator = new MessageULIDGenerator(new ULID());

        final long ts = Tools.nowUTC().getMillis();

        // first seen sequence (gets subtracted with itself)
        ULID.Value parsedULID = ULID.parseULID(generator.createULID("node", "input", ts, 123));
        //noinspection PointlessArithmeticExpression
        assertThat(extractSequenceNr(parsedULID)).isEqualTo(123 - 123 + REORDERING_GAP);

        // second sequence (gets subtracted with first seen sequence)
        parsedULID = ULID.parseULID(generator.createULID("node", "input", ts, 128));
        assertThat(extractSequenceNr(parsedULID)).isEqualTo(128 - 123 + REORDERING_GAP);

        // third sequence (gets subtracted with first seen sequence)
        parsedULID = ULID.parseULID(generator.createULID("node", "input", ts, 125));
        assertThat(extractSequenceNr(parsedULID)).isEqualTo(125 - 123 + REORDERING_GAP);
    }

    @Test
    public void keepsTimestamp() {
        final MessageULIDGenerator generator = new MessageULIDGenerator(new ULID());

        final long ts = Tools.nowUTC().getMillis();
        final String ulid = generator.createULID("node", "input", ts, 123);
        ULID.Value parsedULID = ULID.parseULID(ulid);

        assertThat(parsedULID.timestamp()).isEqualTo(ts);
    }

    @Test
    public void generateWithTooLargeSequenceNr() {
        final MessageULIDGenerator generator = new MessageULIDGenerator(new ULID());
        // prime the subtrahend cache with an initial seq nr
        final int firstSeqNr = 42;
        generator.createULID("node", "input", 0, firstSeqNr);

        // Next simulate maxing out the sequence number space in the ULID (16 bit -> 65535) the result should wrap to REORDERING_GAP
        ULID.Value parsedULID = ULID.parseULID(generator.createULID("node", "input", 0, (int) (firstSeqNr + ULID_RANDOM_MSB_MASK - REORDERING_GAP)));
        assertThat(extractSequenceNr(parsedULID)).isEqualTo(REORDERING_GAP);

        // Once we wrap, we reset the cache to the last wrapped number. The sequence will start from the beginning
        parsedULID = ULID.parseULID(generator.createULID("node", "input", 0, (int) (firstSeqNr + ULID_RANDOM_MSB_MASK - REORDERING_GAP + 1)));
        assertThat(extractSequenceNr(parsedULID)).isEqualTo(REORDERING_GAP + 1);

        // messages with a different timestamp start with a new subtrahend and should get a seqNr with OFFSET_GAP
        parsedULID = ULID.parseULID(generator.createULID("node", "input", 23, (int) (firstSeqNr + ULID_RANDOM_MSB_MASK - REORDERING_GAP)));
        //noinspection PointlessArithmeticExpression
        assertThat(extractSequenceNr(parsedULID)).isEqualTo(0 + REORDERING_GAP);
    }

    @Test
    public void sortedInput() {
        final MessageULIDGenerator generator = new MessageULIDGenerator(new ULID());
        final long ts = Tools.nowUTC().getMillis();

        for (int seq : ImmutableList.of(1, 2, 3, 4)) {
            ULID.Value parsedULID = ULID.parseULID(generator.createULID("node", "input", ts, seq));
            assertThat(parsedULID.timestamp()).isEqualTo(ts);
            assertThat(parsedULID.getMostSignificantBits() & 0xFFFFL).isEqualTo(REORDERING_GAP + seq -1);
        }
    }

    @Test
    public void unorderedInput() {
        final MessageULIDGenerator generator = new MessageULIDGenerator(new ULID());
        final long ts = Tools.nowUTC().getMillis();

        final ImmutableList<Integer> messageSeqences = ImmutableList.of(5, 4, 1, 2);
        final List<String> ulidsSorted = messageSeqences.stream().map((seq) -> generator.createULID("node", "input", ts, seq)).sorted().toList();

        final List<Long> seqNrsFromUlid = ulidsSorted.stream().map(((ulid) -> ULID.parseULID(ulid).getMostSignificantBits() & 0xFFFFL)).collect(Collectors.toList());
        assertThat(seqNrsFromUlid).isEqualTo(messageSeqences.stream().sorted().map(s -> REORDERING_GAP + s - 5L).collect(Collectors.toList()));
    }

    private long extractSequenceNr(ULID.Value ulid) {
        return ulid.getMostSignificantBits() & MessageULIDGenerator.ULID_RANDOM_MSB_MASK;
    }
}

