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
package org.graylog.plugins.views.search.rest.scriptingapi.response;

import org.junit.jupiter.api.Test;

import static org.graylog.plugins.views.search.rest.scriptingapi.response.ResponseEntryDataType.DATE;
import static org.graylog.plugins.views.search.rest.scriptingapi.response.ResponseEntryDataType.GEO;
import static org.graylog.plugins.views.search.rest.scriptingapi.response.ResponseEntryDataType.NUMERIC;
import static org.graylog.plugins.views.search.rest.scriptingapi.response.ResponseEntryDataType.STRING;
import static org.graylog.plugins.views.search.rest.scriptingapi.response.ResponseEntryDataType.UNKNOWN;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ResponseEntryDataTypeTest {

    @Test
    void unknownTypeOnBlankEsType() {
        assertEquals(UNKNOWN, ResponseEntryDataType.fromSearchEngineType(null));
        assertEquals(UNKNOWN, ResponseEntryDataType.fromSearchEngineType(""));
    }

    @Test
    void unknownTypeOnWrongEsType() {
        assertEquals(UNKNOWN, ResponseEntryDataType.fromSearchEngineType("hamburger"));
    }

    @Test
    void properTypeOnProperEsType() {
        assertEquals(NUMERIC, ResponseEntryDataType.fromSearchEngineType("float"));
        assertEquals(NUMERIC, ResponseEntryDataType.fromSearchEngineType("long"));
        assertEquals(STRING, ResponseEntryDataType.fromSearchEngineType("text"));
        assertEquals(STRING, ResponseEntryDataType.fromSearchEngineType("keyword"));
        assertEquals(DATE, ResponseEntryDataType.fromSearchEngineType("date"));
    }

    @Test
    void lowercaseToString() {
        assertEquals("geo", GEO.toString());
    }
}
