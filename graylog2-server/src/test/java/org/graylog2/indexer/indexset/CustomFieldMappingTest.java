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
package org.graylog2.indexer.indexset;

import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomFieldMappingTest {

    @Test
    void testToPhysicalTypeThrowsExceptionWhenWrongTypeUsed() {
        assertThrows(IllegalStateException.class, () -> new CustomFieldMapping("field_name", "bajobongo_type").toPhysicalType());
    }

    @Test
    void testToPhysicalTypeReturnsProperPhysicalStringRepresentations() {
        assertEquals("keyword", new CustomFieldMapping("field_name", "string").toPhysicalType());
        assertEquals("text", new CustomFieldMapping("field_name", "string_fts").toPhysicalType());
    }

    @Test
    void testToPhysicalTypeReturnsProperPhysicalNumericRepresentations() {
        assertEquals("long", new CustomFieldMapping("field_name", "long").toPhysicalType());
        assertEquals("double", new CustomFieldMapping("field_name", "double").toPhysicalType());
    }

    @Test
    void testToFieldTypeDTO() {
        assertEquals(FieldTypeDTO.create("x", "long"),
                new CustomFieldMapping("x", "long").toFieldTypeDTO());
    }
}
