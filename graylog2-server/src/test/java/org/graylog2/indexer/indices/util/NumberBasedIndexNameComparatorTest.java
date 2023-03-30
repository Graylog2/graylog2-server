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
package org.graylog2.indexer.indices.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NumberBasedIndexNameComparatorTest {

    private NumberBasedIndexNameComparator comparator;

    @BeforeEach
    void setUp() {
        comparator = new NumberBasedIndexNameComparator("_");
    }

    @Test
    void indexPrefixIsMoreImportantThanNumberWhileSorting() {
        assertTrue(comparator.compare("abc_5", "bcd_3") < 0);
        assertTrue(comparator.compare("abc", "bcd_3") < 0);
        assertTrue(comparator.compare("zzz_1", "aaa") > 0);
        assertTrue(comparator.compare("zzz", "aaa") > 0);
    }

    @Test
    void comparesDescByNumberAfterLastSeparatorOccurrence() {
        assertTrue(comparator.compare("lalala_5", "lalala_3") < 0);
        assertTrue(comparator.compare("lalala_3", "lalala_5") > 0);

        assertTrue(comparator.compare("lalala_1_5", "lalala_1_3") < 0);
        assertTrue(comparator.compare("lalala_1_5", "lalala_1_0") < 0);
    }

    @Test
    void indexNameWithNoSeparatorGoesLast() {
        assertTrue(comparator.compare("lalala", "lalala_3") > 0);
        assertTrue(comparator.compare("lalala_3", "lalala") < 0);
        assertTrue(comparator.compare("lalala", "lalala_42") > 0);
        assertTrue(comparator.compare("lalala_42", "lalala") < 0);
    }

    @Test
    void isImmuneToWrongNumbersWhichGoLast() {
        assertTrue(comparator.compare("lalala_1!1", "lalala_3") > 0);
        assertTrue(comparator.compare("lalala_3", "lalala_1!1") < 0);
    }

    @Test
    void isImmuneToMissingNumbersWhichGoLast() {
        assertTrue(comparator.compare("lalala_", "lalala_3") > 0);
        assertTrue(comparator.compare("lalala_3", "lalala_") < 0);
    }

}
