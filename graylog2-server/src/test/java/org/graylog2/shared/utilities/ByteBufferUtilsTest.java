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
package org.graylog2.shared.utilities;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class ByteBufferUtilsTest {
    @Test
    public void readBytesFromArrayBackedByteBuffer() {
        final byte[] bytes = "FOOBAR".getBytes(StandardCharsets.US_ASCII);
        final ByteBuffer buffer1 = ByteBuffer.wrap(bytes);
        final ByteBuffer buffer2 = ByteBuffer.wrap(bytes);
        final byte[] readBytesComplete = ByteBufferUtils.readBytes(buffer1);
        final byte[] readBytesPartial = ByteBufferUtils.readBytes(buffer2, 0, 3);

        assertThat(readBytesComplete).isEqualTo(bytes);
        assertThat(readBytesPartial).isEqualTo(Arrays.copyOf(bytes, 3));
    }

    @Test
    public void readBytesFromNonArrayBackedByteBuffer() {
        final byte[] bytes = "FOOBAR".getBytes(StandardCharsets.US_ASCII);
        final ByteBuffer buffer1 = ByteBuffer.allocateDirect(1024);
        buffer1.put(bytes).flip();
        final ByteBuffer buffer2 = ByteBuffer.allocateDirect(1024);
        buffer2.put(bytes).flip();

        final byte[] readBytesComplete = ByteBufferUtils.readBytes(buffer1);
        final byte[] readBytesPartial = ByteBufferUtils.readBytes(buffer2, 0, 3);

        assertThat(readBytesComplete).isEqualTo(bytes);
        assertThat(readBytesPartial).isEqualTo(Arrays.copyOf(bytes, 3));
    }
}