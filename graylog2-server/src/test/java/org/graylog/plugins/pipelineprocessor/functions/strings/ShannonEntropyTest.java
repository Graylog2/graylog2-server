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
package org.graylog.plugins.pipelineprocessor.functions.strings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShannonEntropyTest {
    @Test
    public void testEntropyCalcForChars() {
        assertEquals(0D, ShannonEntropy.calculateForChars("1111"));
        assertEquals(0D, ShannonEntropy.calculateForChars("5555555555"), 0.0D);
        assertEquals(0D, ShannonEntropy.calculateForChars("5555555555"), 0.0D);
        assertEquals(0.46899559358928133D, ShannonEntropy.calculateForChars("1555555555"));
        assertEquals(1.0D, ShannonEntropy.calculateForChars("1111155555"));
        assertEquals(3.3219280948873635D, ShannonEntropy.calculateForChars("1234567890"));
        assertEquals(5.1699250014423095D, ShannonEntropy.calculateForChars("1234567890qwertyuiopasdfghjklzxcvbnm"));
    }
}
