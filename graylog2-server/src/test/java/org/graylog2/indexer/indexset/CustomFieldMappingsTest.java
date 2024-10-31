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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomFieldMappingsTest {

    @Test
    void testReturnsOriginalMappingsIfMergedWithNullMappings() {
        CustomFieldMappings customFieldMappings = new CustomFieldMappings(List.of());
        assertSame(customFieldMappings, customFieldMappings.mergeWith((CustomFieldMappings) null));
    }

    @Test
    void testReturnsOriginalMappingsIfMergedWithEmptyMappings() {
        CustomFieldMappings customFieldMappings = new CustomFieldMappings(List.of());
        assertSame(customFieldMappings, customFieldMappings.mergeWith(new CustomFieldMappings()));
    }

    @Test
    void testMappingsMerging() {
        CustomFieldMappings originalMappings = new CustomFieldMappings(List.of(
                new CustomFieldMapping("not-overriden", "string"),
                new CustomFieldMapping("overriden", "string")
        ));

        CustomFieldMappings overrides = new CustomFieldMappings(List.of(
                new CustomFieldMapping("overriden", "long"),
                new CustomFieldMapping("new", "ip")
        ));

        CustomFieldMappings expected = new CustomFieldMappings(List.of(
                new CustomFieldMapping("not-overriden", "string"),
                new CustomFieldMapping("overriden", "long"),
                new CustomFieldMapping("new", "ip")
        ));

        assertEquals(expected, originalMappings.mergeWith(overrides));
    }

    @Test
    void testContainsMappingForFieldWorksCorrectly() {
        CustomFieldMappings mapping = new CustomFieldMappings(List.of(
                new CustomFieldMapping("field1", "string"),
                new CustomFieldMapping("field2", "long")
        ));

        assertTrue(mapping.containsCustomMappingForField("field1"));
        assertTrue(mapping.containsCustomMappingForField("field2"));
        assertFalse(mapping.containsCustomMappingForField("bubamara!"));
    }
}
