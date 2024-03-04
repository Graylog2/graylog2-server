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

import static org.junit.jupiter.api.Assertions.assertEquals;

class RolloverTimeUnitFormatterTest {

    @Test
    public void testOpenSearchRetentionFormatting() {
        assertEquals("1d", RolloverActionFormatter.formatDaysDuration(1));
        assertEquals("14d", RolloverActionFormatter.formatDaysDuration(14));
        assertEquals("2gb", RolloverActionFormatter.formatGbSize(2));
        assertEquals("30gb", RolloverActionFormatter.formatGbSize(30));
    }
}
