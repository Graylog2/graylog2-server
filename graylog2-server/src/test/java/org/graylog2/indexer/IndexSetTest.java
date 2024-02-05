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
package org.graylog2.indexer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;

class IndexSetTest {

    IndexSet indexSet;

    @BeforeEach
    void setUp() {
        indexSet = Mockito.spy(IndexSet.class);
        doReturn("graylog_13").when(indexSet).getActiveWriteIndex();
        doReturn(Optional.of(13)).when(indexSet).extractIndexNumber("graylog_13");
        doReturn("graylog").when(indexSet).getIndexPrefix();
    }

    @Test
    void testGetNthIndexBeforeActiveIndexSetReturnsProperIndexNamesForProperCalls() {
        assertEquals("graylog_12", indexSet.getNthIndexBeforeActiveIndexSet(1));
        assertEquals("graylog_10", indexSet.getNthIndexBeforeActiveIndexSet(3));
        assertEquals("graylog_0", indexSet.getNthIndexBeforeActiveIndexSet(13));
    }

    @Test
    void testGetNthIndexBeforeActiveIndexSetReturnsNullForImproperCalls() {
        assertNull(indexSet.getNthIndexBeforeActiveIndexSet(14));
        assertNull(indexSet.getNthIndexBeforeActiveIndexSet(50));
    }

    @Test
    void testGetNthIndexBeforeActiveIndexSetReturnsNullWhenDeflectorIndexNumberCannotBeExtracted() {
        doReturn(Optional.empty()).when(indexSet).extractIndexNumber("graylog_13");
        assertNull(indexSet.getNthIndexBeforeActiveIndexSet(1));
    }
}
