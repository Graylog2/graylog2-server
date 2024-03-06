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
package org.graylog2.rest.resources.system.indexer.responses;

import org.graylog2.rest.resources.entities.Sorting;
import org.junit.jupiter.api.Test;

import java.util.Comparator;

import static org.graylog2.rest.resources.system.indexer.responses.FieldTypeOrigin.INDEX;
import static org.graylog2.rest.resources.system.indexer.responses.IndexSetFieldType.FIELD_NAME;
import static org.graylog2.rest.resources.system.indexer.responses.IndexSetFieldType.TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexSetFieldTypeTest {

    @Test
    void testComparatorOnFieldName() {

        final Comparator<IndexSetFieldType> fieldNameComparator = IndexSetFieldType.getComparator(FIELD_NAME, Sorting.Direction.ASC);
        final Comparator<IndexSetFieldType> reversedFieldNameComparator = IndexSetFieldType.getComparator(FIELD_NAME, Sorting.Direction.DESC);
        assertEquals(0, fieldNameComparator.compare(
                new IndexSetFieldType("buhaha", "long", INDEX, false),
                new IndexSetFieldType("buhaha", "string", INDEX, false)
        ));
        assertEquals(0, reversedFieldNameComparator.compare(
                new IndexSetFieldType("buhaha", "long", INDEX, false),
                new IndexSetFieldType("buhaha", "string", INDEX, false)
        ));
        assertTrue(0 > fieldNameComparator.compare(
                new IndexSetFieldType("buhaha", "long", INDEX, false),
                new IndexSetFieldType("chiquita", "string", INDEX, false)
        ));
        assertTrue(0 < reversedFieldNameComparator.compare(
                new IndexSetFieldType("buhaha", "long", INDEX, false),
                new IndexSetFieldType("chiquita", "string", INDEX, false)
        ));
        assertTrue(0 < fieldNameComparator.compare(
                new IndexSetFieldType("buhaha", "long", INDEX, false),
                new IndexSetFieldType("arizona", "string", INDEX, false)
        ));
        assertTrue(0 > reversedFieldNameComparator.compare(
                new IndexSetFieldType("buhaha", "long", INDEX, false),
                new IndexSetFieldType("arizona", "string", INDEX, false)
        ));
    }

    @Test
    void testComparatorOnFieldType() {

        final Comparator<IndexSetFieldType> fieldTypeComparator = IndexSetFieldType.getComparator(TYPE, Sorting.Direction.ASC);
        final Comparator<IndexSetFieldType> reversedFieldTypeComparator = IndexSetFieldType.getComparator(TYPE, Sorting.Direction.DESC);
        assertEquals(0, fieldTypeComparator.compare(
                new IndexSetFieldType("buhaha", "long", INDEX, false),
                new IndexSetFieldType("buhaha", "long", INDEX, false)
        ));
        assertEquals(0, reversedFieldTypeComparator.compare(
                new IndexSetFieldType("buhaha", "long", INDEX, false),
                new IndexSetFieldType("buhaha", "long", INDEX, false)
        ));
        assertTrue(0 > fieldTypeComparator.compare(
                new IndexSetFieldType("buhaha", "long", INDEX, false),
                new IndexSetFieldType("chiquita", "string", INDEX, false)
        ));
        assertTrue(0 < reversedFieldTypeComparator.compare(
                new IndexSetFieldType("buhaha", "long", INDEX, false),
                new IndexSetFieldType("chiquita", "string", INDEX, false)
        ));
        assertTrue(0 < fieldTypeComparator.compare(
                new IndexSetFieldType("buhaha", "text", INDEX, false),
                new IndexSetFieldType("arizona", "string", INDEX, false)
        ));
        assertTrue(0 > reversedFieldTypeComparator.compare(
                new IndexSetFieldType("buhaha", "text", INDEX, false),
                new IndexSetFieldType("arizona", "string", INDEX, false)
        ));
    }
}
