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
package org.graylog.plugins.views.search.engine.monitoring.data.histogram.creation;

import org.graylog.plugins.views.search.engine.QueryExecutionStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PercentageValueComputationTest {
    ValueComputation<QueryExecutionStats, Long> toTest;

    @BeforeEach
    void setUp() {
        toTest = new PercentageValueComputation();
    }

    @Test
    void testReturnsZeroOnNoData() {
        final Long result = toTest.computeValue(List.of(), 42);
        assertEquals(0, result);
    }

    @Test
    void testReturnsProperPercentage() {
        final Long result = toTest.computeValue(
                List.of(
                        QueryExecutionStats.builder().duration(13).build()
                ),
                50
        );
        assertEquals(2, result); //1 out of 50 is 2%
    }
}
