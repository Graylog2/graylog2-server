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
package org.graylog.plugins.cef.parser;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CEFParserTest {
    private final CEFParser parser = new CEFParser();

    @Test
    void hostnameContainingACefPrefixIsNotMistakenForTheHeader() {
        final CEFMessage message =
                parser.parse("2026-09-03T16:35:24Z host-CEF:1-box CEF:0|Vendor|Product|1.0|100|Name|5|msg=x");

        assertEquals("host-CEF:1-box", message.host());
        assertEquals("Name", message.name());
    }

    @Test
    void leadingSyslogVersionDigitIsNotReadAsAnEpochTimestamp() {
        assertThrows(IllegalStateException.class, () -> parser.parse(
                "1 2 collector CEF:0|Vendor|Product|1.0|100|Name|5|msg=x"));
    }

    @Test
    void epochMillisecondsAreStillAccepted() {
        final CEFMessage message =
                parser.parse("1788928524921 collector CEF:0|Vendor|Product|1.0|100|Name|5|msg=x");

        assertEquals(new Date(1788928524921L), message.timestamp());
        assertEquals("collector", message.host());
    }
}
