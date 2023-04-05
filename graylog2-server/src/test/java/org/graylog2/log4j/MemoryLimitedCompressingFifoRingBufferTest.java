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
package org.graylog2.log4j;

import org.bouncycastle.util.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MemoryLimitedCompressingFifoRingBufferTest {

    private MemoryLimitedCompressingFifoRingBuffer buffer;

    @BeforeEach
    void setUp() {
        buffer = new MemoryLimitedCompressingFifoRingBuffer(1024 * 2);
    }

    @Test
    void addSingle() throws IOException {
        buffer.add("Foo".getBytes(StandardCharsets.UTF_8));

        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        buffer.streamContent(outStream, 0);

        assertThat(outStream.toString(StandardCharsets.UTF_8)).isEqualTo("Foo");
    }

    @Test
    void addBeyondCacheSize() throws IOException {
        final int count = MemoryLimitedCompressingFifoRingBuffer.BATCHSIZE * 2 + 10;
        for (int i = 1; i < count; i++) {
            buffer.add(("Loop " + i + "\n").getBytes(StandardCharsets.UTF_8));
        }

        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        buffer.streamContent(outStream, 0);

        final List<String> result = List.of(Strings.split(outStream.toString(StandardCharsets.UTF_8), '\n'));
        assertThat(result).hasSize(count);
        assertThat(result.get(0)).isEqualTo("Loop 1");
        assertThat(result.get(result.size() - 2)).isEqualTo("Loop " + (count - 1));
    }

    @Test
    void compressedRotation() throws IOException {
        final int count = MemoryLimitedCompressingFifoRingBuffer.BATCHSIZE * 7;
        for (int i = 1; i < count; i++) {
            buffer.add(("Loop " + i + "\n").getBytes(StandardCharsets.UTF_8));
        }

        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        buffer.streamContent(outStream, 0);

        final List<String> result = List.of(Strings.split(outStream.toString(StandardCharsets.UTF_8), '\n'));
        assertThat(result.size()).isLessThan(count);

        // assert that newest entries are kept while older ones are removed
        assertThat(result.get(0)).isNotEqualTo("Loop 1");
        assertThat(result.get(result.size() - 2)).isEqualTo("Loop " + (count - 1));
    }

    @Test
    void limitedStream() throws IOException {
        final int count = MemoryLimitedCompressingFifoRingBuffer.BATCHSIZE * 4 + 1;
        for (int i = 1; i < count; i++) {
            buffer.add(("Loop " + i + "\n").getBytes(StandardCharsets.UTF_8));
        }

        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        buffer.streamContent(outStream, 1);

        List<String> result = List.of(Strings.split(outStream.toString(StandardCharsets.UTF_8), '\n'));
        assertThat(result.size()).isEqualTo(MemoryLimitedCompressingFifoRingBuffer.BATCHSIZE + 1);

        buffer.streamContent(outStream, 2);
        result = List.of(Strings.split(outStream.toString(StandardCharsets.UTF_8), '\n'));
        assertThat(result.size()).isEqualTo(MemoryLimitedCompressingFifoRingBuffer.BATCHSIZE * 2 + 1);
    }

    @Test
    @Disabled
    void benchmark() throws IOException {
        final int count = MemoryLimitedCompressingFifoRingBuffer.BATCHSIZE * 100000;
        for (int i = 1; i < count; i++) {
            buffer.add(("Loop " + i + "\n").getBytes());
        }
    }
}
