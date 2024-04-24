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
package org.graylog.plugins.views.search.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListOfStringsComparatorTest {

    ListOfStringsComparator toTest;

    @BeforeEach
    void setUp() {
        toTest = new ListOfStringsComparator();
    }

    @Test
    void testEmptyListsAreEqual() {
        assertEquals(0, toTest.compare(List.of(), List.of()));
    }

    @Test
    void testListsWithTheSameElementsAreEqual() {
        assertEquals(0, toTest.compare(List.of("mum"), List.of("mum")));
        assertEquals(0, toTest.compare(List.of("mum", "dad"), List.of("mum", "dad")));
    }

    @Test
    void testListsWithSameElementsIgnoringCaseAreEqual() {
        assertEquals(0, toTest.compare(List.of("Mum"), List.of("mum")));
        assertEquals(0, toTest.compare(List.of("mum", "Dad"), List.of("mum", "dad")));
    }

    @Test
    void testEmptyListIsSmallerThanListWithElements() {
        assertTrue(toTest.compare(List.of(), List.of("mum")) < 0);
        assertTrue(toTest.compare(List.of("mum", "Dad"), List.of()) > 0);
    }

    @Test
    void testShorterListIsSmaller() {
        assertTrue(toTest.compare(List.of("max(carramba)"), List.of("GET", "max(carramba)")) < 0);
    }

    @Test
    void testListOfSameSizeAreOrderedBasedOnTheirFirstNonEqualElement() {
        assertTrue(toTest.compare(List.of("mum", "dad"), List.of("mum", "plumber")) < 0); //last elem decides
        assertTrue(toTest.compare(List.of("mummy", "dad", ""), List.of("mum", "dad", "whatever")) > 0); //first elem decides
    }
}
