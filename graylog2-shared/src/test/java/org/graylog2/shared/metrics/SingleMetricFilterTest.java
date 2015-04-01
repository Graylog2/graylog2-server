/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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