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
import org.junit.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.shared.buffers.processors.MessageULIDGenerator.OFFSET_GAP;
import static org.graylog2.shared.buffers.processors.MessageULIDGenerator.RANDOM_MSB_MASK;

public class MessageULIDGeneratorTest {

    @Test
    public void simpleGenerate() {
        final MessageULIDGenerator generator = new MessageULIDGenerator(new ULID());

        final String ulid = generator.createULID("input", 0, 123);
        assertThat(ulid).startsWith("000000000007T");
        ULID.Value parsedULID = ULID.parseULID(ulid);

        // first seen sequence (gets subtracted with itself)
        assertThat(extractSequenceNr(parsedULID)).isEqualTo(123 - 123 + OFFSET_GAP);

        // second sequence (gets subtracted with first seen sequence)
        parsedULID = ULID.parseULID(generator.createULID("input", 0, 128));
        assertThat(extractSequenceNr(parsedULID)).isEqualTo(128 - 123 + OFFSET_GAP);

        // third sequence (gets subtracted with first seen sequence)
        parsedULID = ULID.parseULID(generator.createULID("input", 0, 125));
        assertThat(extractSequenceNr(parsedULID)).isEqualTo(125 - 123 + OFFSET_GAP);
    }

    @Test
    public void generateWithTooLargeSequenceNr() {
        final MessageULIDGenerator generator = new MessageULIDGenerator(new ULID());
        // prime the subtrahend cache with an initial seq nr
        final int firstSeqNr = 42;
        generator.createULID("input", 0, firstSeqNr);

        // Next simulate maxing out the sequence number space in the ULID (16 bit -> 65535) the result should wrap to 0
        ULID.Value parsedULID = ULID.parseULID(generator.createULID("input", 0, (int) (firstSeqNr + RANDOM_MSB_MASK - OFFSET_GAP)));
        assertThat(extractSequenceNr(parsedULID)).isEqualTo(0);

        // messages with a different timestamp start with a new subtrahend and should get a seqNr with OFFSET_GAP
        parsedULID = ULID.parseULID(generator.createULID("input", 42, (int) (firstSeqNr + RANDOM_MSB_MASK - OFFSET_GAP)));
        assertThat(extractSequenceNr(parsedULID)).isEqualTo(0 + OFFSET_GAP);
    }

    @Test
    public void sortedInput() {
        final MessageULIDGenerator generator = new MessageULIDGenerator(new ULID());
        final long ts = Instant.now().toEpochMilli();

        for (int seq : ImmutableList.of(1, 2, 3, 4)) {
            ULID.Value parsedULID = ULID.parseULID(generator.createULID("input", ts, seq));
            assertThat(parsedULID.timestamp()).isEqualTo(ts);
            assertThat(parsedULID.getMostSignificantBits() & 0xFFFFL).isEqualTo(OFFSET_GAP + seq -1);
        }
    }

    @Test
    public void unorderedInput() {
        final MessageULIDGenerator generator = new MessageULIDGenerator(new ULID());
        final long ts = Instant.now().toEpochMilli();

        final ImmutableList<Integer> messageSeqences = ImmutableList.of(5, 4, 1, 2);
        final List<String> ulidsSorted = messageSeqences.stream().map((seq) -> generator.createULID("input", ts, seq)).sorted().collect(Collectors.toList());

        final List<Long> seqNrsFromUlid = ulidsSorted.stream().map(((ulid) -> ULID.parseULID(ulid).getMostSignificantBits() & 0xFFFFL)).collect(Collectors.toList());
        assertThat(seqNrsFromUlid).isEqualTo(messageSeqences.stream().sorted().map(s -> OFFSET_GAP + s - 5L).collect(Collectors.toList()));
    }

    private long extractSequenceNr(ULID.Value ulid) {
        return ulid.getMostSignificantBits() & MessageULIDGenerator.RANDOM_MSB_MASK;
    }
}

