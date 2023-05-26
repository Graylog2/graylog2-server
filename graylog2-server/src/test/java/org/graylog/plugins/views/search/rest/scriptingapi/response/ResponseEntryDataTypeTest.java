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

import org.graylog2.indexer.fieldtypes.FieldTypeMapper;
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
        assertEquals(UNKNOWN, ResponseEntryDataType.fromFieldType(null));
    }

    @Test
    void properTypeOnProperEsType() {
        assertEquals(NUMERIC, ResponseEntryDataType.fromFieldType(FieldTypeMapper.FLOAT_TYPE));
        assertEquals(NUMERIC, ResponseEntryDataType.fromFieldType(FieldTypeMapper.LONG_TYPE));
        assertEquals(STRING, ResponseEntryDataType.fromFieldType(FieldTypeMapper.STRING_FTS_TYPE));
        assertEquals(STRING, ResponseEntryDataType.fromFieldType(FieldTypeMapper.STRING_TYPE));
        assertEquals(DATE, ResponseEntryDataType.fromFieldType(FieldTypeMapper.DATE_TYPE));
    }

    @Test
    void lowercaseToString() {
        assertEquals("geo", GEO.toString());
    }
}
