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
package org.graylog2.shared.metrics;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SingleMetricFilterTest {

    @Test
    public void testMatches() throws Exception {
        final MetricUtils.SingleMetricFilter filtersAllowed = new MetricUtils.SingleMetricFilter("allowed");

        // metric is not used and can be null
        assertTrue(filtersAllowed.matches("allowed", null));
        // the match is case sensitive
        assertFalse(filtersAllowed.matches("Allowed", null));
        // the name must match
        assertFalse(filtersAllowed.matches("disallowed", null));

    }
}