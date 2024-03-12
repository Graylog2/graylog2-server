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
package org.graylog2.indexer.datastream.policy.actions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BytesUnitTest {

    @Test
    public void testFormat() {
        assertEquals("10pb", BytesUnit.PEBIBYTES.format(10L));
        assertEquals("10tb", BytesUnit.TEBIBYTES.format(10L));
        assertEquals("10gb", BytesUnit.GIBIBYTES.format(10L));
        assertEquals("10mb", BytesUnit.MEBIBYTES.format(10L));
        assertEquals("10kb", BytesUnit.KIBIBYTES.format(10L));
        assertEquals("10b", BytesUnit.BYTES.format(10L));
    }
}
