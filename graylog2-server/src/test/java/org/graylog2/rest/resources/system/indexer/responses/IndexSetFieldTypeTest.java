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

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.rest.resources.system.indexer.responses.FieldTypeOrigin.INDEX;
import static org.graylog2.rest.resources.system.indexer.responses.IndexSetFieldType.FIELD_NAME;
import static org.graylog2.rest.resources.system.indexer.responses.IndexSetFieldType.TYPE;

class IndexSetFieldTypeTest {

    @Test
    void testComparatorOnFieldName() {

        final Comparator<IndexSetFieldType> fieldNameComparator = IndexSetFieldType.getComparator(FIELD_NAME, Sorting.Direction.ASC);
        final Comparator<IndexSetFieldType> reversedFieldNameComparator = IndexSetFieldType.getComparator(FIELD_NAME, Sorting.Direction.DESC);
        assertThat(fieldNameComparator.compare(
                new IndexSetFieldType("buhaha", "long", INDEX, false),
                new IndexSetFieldType("buhaha", "string", INDEX, false)
        )).isZero();
        assertThat(reversedFieldNameComparator.compare(
                new IndexSetFieldType("buhaha", "long", INDEX, false),
                new IndexSetFieldType("buhaha", "string", INDEX, false)
        )).isZero();
        assertThat(fieldNameComparator.compare(
                new IndexSetFieldType("buhaha", "long", INDEX, false),
                new IndexSetFieldType("chiquita", "string", INDEX, false)
        )).isNegative();
        assertThat(reversedFieldNameComparator.compare(
                new IndexSetFieldType("buhaha", "long", INDEX, false),
                new IndexSetFieldType("chiquita", "string", INDEX, false)
        )).isPositive();
        assertThat(fieldNameComparator.compare(
                new IndexSetFieldType("buhaha", "long", INDEX, false),
                new IndexSetFieldType("arizona", "string", INDEX, false)
        )).isPositive();
        assertThat(reversedFieldNameComparator.compare(
                new IndexSetFieldType("buhaha", "long", INDEX, false),
                new IndexSetFieldType("arizona", "string", INDEX, false)
        )).isNegative();
    }

    @Test
    void testComparatorOnFieldType() {

        final Comparator<IndexSetFieldType> fieldTypeComparator = IndexSetFieldType.getComparator(TYPE, Sorting.Direction.ASC);
        final Comparator<IndexSetFieldType> reversedFieldTypeComparator = IndexSetFieldType.getComparator(TYPE, Sorting.Direction.DESC);
        assertThat(fieldTypeComparator.compare(
                new IndexSetFieldType("buhaha", "long", INDEX, false),
                new IndexSetFieldType("buhaha", "long", INDEX, false)
        )).isZero();
        assertThat(reversedFieldTypeComparator.compare(
                new IndexSetFieldType("buhaha", "long", INDEX, false),
                new IndexSetFieldType("buhaha", "long", INDEX, false)
        )).isZero();
        assertThat(fieldTypeComparator.compare(
                new IndexSetFieldType("buhaha", "long", INDEX, false),
                new IndexSetFieldType("chiquita", "string", INDEX, false)
        )).isNegative();
        assertThat(reversedFieldTypeComparator.compare(
                new IndexSetFieldType("buhaha", "long", INDEX, false),
                new IndexSetFieldType("chiquita", "string", INDEX, false)
        )).isPositive();
        assertThat(fieldTypeComparator.compare(
                new IndexSetFieldType("buhaha", "text", INDEX, false),
                new IndexSetFieldType("arizona", "string", INDEX, false)
        )).isPositive();
        assertThat(reversedFieldTypeComparator.compare(
                new IndexSetFieldType("buhaha", "text", INDEX, false),
                new IndexSetFieldType("arizona", "string", INDEX, false)
        )).isNegative();

        assertThat(fieldTypeComparator.compare(
                new IndexSetFieldType("buhaha", "long", INDEX, false),
                new IndexSetFieldType("buhaha", null, INDEX, false)
        )).isNegative();
        assertThat(reversedFieldTypeComparator.compare(
                new IndexSetFieldType("buhaha", "long", INDEX, false),
                new IndexSetFieldType("buhaha", null, INDEX, false)
        )).isPositive();
    }
}
